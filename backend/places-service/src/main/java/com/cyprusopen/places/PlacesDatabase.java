package com.cyprusopen.places;

import com.cyprusopen.common.db.ModuleDatabase;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The places database (parking, beaches, air quality). It keeps the {@code parking} name, role and
 * {@code PARKING_*} variables from before places-service, so no data or secrets move.
 */
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:places-service.properties")
public class PlacesDatabase {
    public static final String JDBC = "placesJdbc";
    public static final String LIQUIBASE = "placesLiquibase";
    private static final String DATA_SOURCE = "placesDataSource";

    @Bean(DATA_SOURCE)
    HikariDataSource dataSource(Environment environment) {
        return ModuleDatabase.dataSource(environment, "parking");
    }

    @Bean(LIQUIBASE)
    SpringLiquibase liquibase(@Qualifier(DATA_SOURCE) DataSource dataSource, Environment environment) {
        return ModuleDatabase.liquibase(dataSource, environment, "parking", PlacesDatabase.class);
    }

    @Bean(JDBC)
    JdbcClient jdbc(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
