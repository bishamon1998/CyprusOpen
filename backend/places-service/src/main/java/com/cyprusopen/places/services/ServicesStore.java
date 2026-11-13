package com.cyprusopen.places.services;

import com.cyprusopen.places.JsonDocumentStore;
import com.cyprusopen.places.PlacesDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest hospital and Citizen Service Centre snapshot ({@code service_places} table). */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class ServicesStore extends JsonDocumentStore<ServicesSnapshot> {
    @Autowired
    ServicesStore(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "service_places", ServicesSnapshot.class);
    }
}
