package com.cyprusopen.common.db;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * The database of one module (statistics, places, pharmacies) when several run in one application. Each module
 * keeps its own PostgreSQL database, role, connection pool, Liquibase changelog and transactions.
 *
 * <p>Properties: {@code <prefix>.datasource.url}, {@code .username}, {@code .password} and {@code .schema}.
 */
public final class ModuleDatabase {
    /** Every module keeps its changelog at this path, so Liquibase records stay as they were when it ran alone. */
    static final String CHANGELOG_FOLDER = "db/changelog/";
    private static final String CHANGELOG = "classpath:" + CHANGELOG_FOLDER + "db.changelog-master.yaml";

    private ModuleDatabase() {}

    /** A small pool: traffic is low, and every idle connection costs memory in Postgres. */
    public static HikariDataSource dataSource(Environment environment, String prefix) {
        var dataSource = new HikariDataSource();
        dataSource.setPoolName(prefix);
        dataSource.setJdbcUrl(environment.getRequiredProperty(prefix + ".datasource.url"));
        dataSource.setUsername(environment.getRequiredProperty(prefix + ".datasource.username"));
        dataSource.setPassword(environment.getProperty(prefix + ".datasource.password", ""));
        dataSource.setMaximumPoolSize(4);
        dataSource.setMinimumIdle(1);
        return dataSource;
    }

    /**
     * Runs the module's own changelog. All modules use the same changelog path, so the changelog is read only from
     * the jar (or classes folder) that contains {@code module}; the recorded file names stay unchanged.
     */
    public static SpringLiquibase liquibase(DataSource dataSource, Environment environment, String prefix,
                                            Class<?> module) {
        var resources = new DefaultResourceLoader(new ModuleResources(module));
        var liquibase = new SpringLiquibase() {
            @Override
            public void setResourceLoader(ResourceLoader ignored) {
                // Spring injects the application's loader after creation; keep the module-only one.
                super.setResourceLoader(resources);
            }
        };
        liquibase.setResourceLoader(resources);
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGELOG);
        liquibase.setDefaultSchema(environment.getRequiredProperty(prefix + ".datasource.schema"));
        return liquibase;
    }

    /** Loads classes as usual but finds changelog files only in the module's own jar or classes folder. */
    private static final class ModuleResources extends ClassLoader {
        private final URLClassLoader module;

        ModuleResources(Class<?> anchor) {
            super(anchor.getClassLoader());
            var location = anchor.getProtectionDomain().getCodeSource().getLocation();
            module = new URLClassLoader(new URL[] {location}, null);
        }

        @Override
        public URL getResource(String name) {
            return name.startsWith(CHANGELOG_FOLDER) ? module.findResource(name) : super.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) throws IOException {
            return name.startsWith(CHANGELOG_FOLDER) ? module.findResources(name) : super.getResources(name);
        }
    }
}
