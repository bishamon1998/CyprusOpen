package com.cyprusopen.pharmacies;

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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/** The pharmacies database (role {@code pharmacies_app}), separate from the other modules' databases. */
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:pharmacies-service.properties")
class PharmaciesDatabase {
    static final String JDBC = "pharmaciesJdbc";
    static final String LIQUIBASE = "pharmaciesLiquibase";
    static final String TRANSACTIONS = "pharmaciesTransactions";
    private static final String DATA_SOURCE = "pharmaciesDataSource";

    @Bean(DATA_SOURCE)
    HikariDataSource dataSource(Environment environment) {
        return ModuleDatabase.dataSource(environment, "pharmacies");
    }

    @Bean(LIQUIBASE)
    SpringLiquibase liquibase(@Qualifier(DATA_SOURCE) DataSource dataSource, Environment environment) {
        return ModuleDatabase.liquibase(dataSource, environment, "pharmacies", PharmaciesDatabase.class);
    }

    @Bean(JDBC)
    JdbcClient jdbc(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean(TRANSACTIONS)
    DataSourceTransactionManager transactions(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
