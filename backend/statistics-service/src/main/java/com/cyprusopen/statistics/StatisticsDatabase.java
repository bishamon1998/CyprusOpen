package com.cyprusopen.statistics;

import com.cyprusopen.common.db.ModuleDatabase;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/** The statistics database (role {@code statistics_app}), separate from the other modules' databases. */
@Configuration(proxyBeanMethods = false)
@PropertySource("classpath:statistics-service.properties")
class StatisticsDatabase {
    static final String JDBC = "statisticsJdbc";
    static final String BATCH = "statisticsBatch";
    static final String LIQUIBASE = "statisticsLiquibase";
    static final String TRANSACTIONS = "statisticsTransactions";
    private static final String DATA_SOURCE = "statisticsDataSource";

    @Bean(DATA_SOURCE)
    HikariDataSource dataSource(Environment environment) {
        return ModuleDatabase.dataSource(environment, "statistics");
    }

    @Bean(LIQUIBASE)
    SpringLiquibase liquibase(@Qualifier(DATA_SOURCE) DataSource dataSource, Environment environment) {
        return ModuleDatabase.liquibase(dataSource, environment, "statistics", StatisticsDatabase.class);
    }

    @Bean(JDBC)
    JdbcClient jdbc(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean(BATCH)
    JdbcTemplate batch(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(TRANSACTIONS)
    DataSourceTransactionManager transactions(@Qualifier(DATA_SOURCE) DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
