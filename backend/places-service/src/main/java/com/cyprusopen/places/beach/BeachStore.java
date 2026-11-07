package com.cyprusopen.places.beach;

import com.cyprusopen.places.JsonDocumentStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** The latest bathing water snapshot ({@code beaches} table). */
@Repository
@DependsOn("liquibase")
class BeachStore extends JsonDocumentStore<BeachSnapshot> {
    @Autowired
    BeachStore(JdbcClient jdbc, JsonMapper mapper) {
        super(jdbc, mapper, "beaches", BeachSnapshot.class);
    }
}
