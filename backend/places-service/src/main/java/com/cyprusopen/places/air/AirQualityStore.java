package com.cyprusopen.places.air;

import com.cyprusopen.places.JsonDocumentStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest air quality snapshot ({@code air_quality} table). */
@Repository
@DependsOn("liquibase")
class AirQualityStore extends JsonDocumentStore<AirQualitySnapshot> {
    @Autowired
    AirQualityStore(JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "air_quality", AirQualitySnapshot.class);
    }
}
