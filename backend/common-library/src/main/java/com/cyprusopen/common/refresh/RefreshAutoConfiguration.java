package com.cyprusopen.common.refresh;

import java.util.List;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/** Loads the shared refresh schedule and runs the service's {@link RefreshJob}s. */
@AutoConfiguration
@PropertySource("classpath:cyprus-open-refresh.properties")
@EnableConfigurationProperties(RefreshProperties.class)
public class RefreshAutoConfiguration {
    @Bean
    RefreshScheduler refreshScheduler(List<RefreshJob> jobs, RefreshProperties properties) {
        return new RefreshScheduler(jobs, properties);
    }
}
