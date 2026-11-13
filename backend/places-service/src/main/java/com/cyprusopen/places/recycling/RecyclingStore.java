package com.cyprusopen.places.recycling;

import com.cyprusopen.places.JsonDocumentStore;
import com.cyprusopen.places.PlacesDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest Green Dot points snapshot ({@code recycling_points} table). */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class RecyclingStore extends JsonDocumentStore<RecyclingSnapshot> {
    @Autowired
    RecyclingStore(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "recycling_points", RecyclingSnapshot.class);
    }
}
