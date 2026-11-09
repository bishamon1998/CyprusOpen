package com.cyprusopen.places.nature;

import com.cyprusopen.places.JsonDocumentStore;
import com.cyprusopen.places.PlacesDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest picnic, camping and trail snapshot ({@code nature_sites} table). */
@Repository
@DependsOn(PlacesDatabase.LIQUIBASE)
class NatureStore extends JsonDocumentStore<NatureSnapshot> {
    @Autowired
    NatureStore(@Qualifier(PlacesDatabase.JDBC) JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "nature_sites", NatureSnapshot.class);
    }
}
