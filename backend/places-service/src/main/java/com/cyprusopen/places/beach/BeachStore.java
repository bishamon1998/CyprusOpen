package com.cyprusopen.places.beach;

import com.cyprusopen.places.JsonDocumentStore;
import com.cyprusopen.places.PlacesDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest bathing water snapshot ({@code beaches} table). */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class BeachStore extends JsonDocumentStore<BeachSnapshot> {
    @Autowired
    BeachStore(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "beaches", BeachSnapshot.class);
    }
}
