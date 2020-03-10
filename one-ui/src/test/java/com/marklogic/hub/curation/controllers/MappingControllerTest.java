package com.marklogic.hub.curation.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.FailedRequestException;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.hub.ApplicationConfig;
import com.marklogic.hub.oneui.Application;
import com.marklogic.hub.oneui.TestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, ApplicationConfig.class})
public class MappingControllerTest {

    @Autowired
    MappingController controller;

    @Autowired
    TestHelper testHelper;

    static final String MAPPING_CONFIG_1 = "{\n" +
        "  \"name\": \"TestCustomerMapping\",\n" +
        "  \"targetEntity\": \"Customer\",\n" +
        "  \"description\": \"TestCustomerMapping does ...\",\n" +
        "  \"selectedSource\":\"query\",\n" +
        "  \"sourceQuery\": \"cts.CollectionQuery('RAW-CUSTOMER')\",\n" +
        "  \"collections\": []\n" +
        "}";

    static final String MAPPING_CONFIG_2 = "{\n" +
        "  \"name\": \"TestOrderMapping1\",\n" +
        "  \"targetEntity\": \"Order\",\n" +
        "  \"description\": \"TestOrderMapping1 does ...\",\n" +
        "  \"selectedSource\": \"collection\",\n" +
        "  \"sourceQuery\": \"\",\n" +
        "  \"collections\": [\"RAW-ORDER\"]\n" +
        "}";

    static final String MAPPING_CONFIG_3 = "{\n" +
        "  \"name\" : \"TestOrderMapping2\",\n" +
        "  \"targetEntity\" : \"Order\",\n" +
        "  \"description\" : \"TestOrderMapping2 does ...\",\n" +
        "  \"selectedSource\": \"query\",\n" +
        "  \"sourceQuery\": \"cts.CollectionQuery('RAW-ORDER')\",\n" +
        "  \"collections\": []\n" +
        "}";

    static final String MAPPING_SETTINGS = "{\n"
            + "    \"artifactName\" : \"TestCustomerMapping\",\n"
            + "    \"additionalCollections\" : [ \"Collection1\", \"Collection2\" ],\n"
            + "    \"targetDatabase\" : \"data-hub-STAGING\",\n"
            + "    \"permissions\" : \"data-hub-load-data-reader,read,data-hub-load-data-writer,update\",\n"
            +"     \"provenanceGranularity\": \"coarse-grained\",\n"
            + "    \"customHook\" : {\n"
            + "          \"module\" : \"\",\n"
            + "          \"parameters\" : \"\",\n"
            + "          \"user\" : \"\",\n"
            + "          \"runBefore\" : false\n"
            + "    }}";
    @Test
    void testMappingConfigs() throws IOException {
        testHelper.authenticateSession();
        ObjectMapper om = new ObjectMapper();

        // Add entities for mappings
        DocumentMetadataHandle meta = new DocumentMetadataHandle();
        meta.getCollections().add("http://marklogic.com/entity-services/models");
        meta.getPermissions().add("data-hub-developer", DocumentMetadataHandle.Capability.READ, DocumentMetadataHandle.Capability.UPDATE);
        testHelper.addStagingDoc("/entities/Customer.entity.json", meta, "entities/Customer.entity.json");
        testHelper.addStagingDoc("/entities/Order.entity.json", meta, "entities/Order.entity.json");

        controller.updateArtifact("TestCustomerMapping", om.readTree(MAPPING_CONFIG_1));
        controller.updateArtifact("TestOrderMapping1", om.readTree(MAPPING_CONFIG_2));
        controller.updateArtifact("TestOrderMapping2", om.readTree(MAPPING_CONFIG_3));

        ArrayNode configsGroupbyEntity = controller.getArtifacts().getBody();

        assertTrue(configsGroupbyEntity.size() >= 2, "The group entity count of mapping configs should be greater than 2.");

        configsGroupbyEntity.forEach(e -> {
            String currEntityName = e.get("entityType").asText();
            if ("Customer".equals(currEntityName) || "Order".equals(currEntityName)) {
                JsonNode mappingNode = e.get("artifacts");
                assertTrue(e.get("artifacts").size() > 0, String.format("Should have at least 1 mapping config associated with the entity (%s).", currEntityName));
                if (mappingNode instanceof ArrayNode) {
                    boolean found = false;
                    int mapConfigCount = 0;
                    int i = 0;
                    for (; i < mappingNode.size(); ++i) {
                        String mappedEntityName = mappingNode.get(i).get("targetEntity").asText();
                        String mapName = mappingNode.get(i).get("name").asText();
                        if (("Customer".equals(currEntityName) && currEntityName.equals(mappedEntityName) && "TestCustomerMapping".equals(mapName))
                            || ("Order".equals(currEntityName) && currEntityName.equals(mappedEntityName)
                            && ("TestOrderMapping1".equals(mapName) || "TestOrderMapping2".equals(mapName)))) {
                            found = true;
                            ++mapConfigCount;
                        }
                    }
                    assertTrue(found, String.format("Could not find the entity name (%s)", currEntityName));
                    if ("Customer".equals(currEntityName)) {
                        assertEquals(1, mapConfigCount, "Should have 1 mapping config associate with the entity (Customer).");
                    } else { //Order
                        assertEquals(2, mapConfigCount, "Should have 2 mapping configs associate with the entity (Order).");
                    }
                } else if (mappingNode instanceof ObjectNode) {
                    assertEquals(currEntityName, mappingNode.get("targetEntity").asText(), "mismatch entity name.");
                    if ("Customer".equals(currEntityName)) {
                        assertEquals("TestCustomerMapping", mappingNode.get("name").asText(), "mismatch mapping name.");
                    } else { //Order
                        fail("Should have 2 mapping configs associate with the entity (Order).");
                    }
                } else {
                    fail("error data type!");
                }
            }
        });

        controller.deleteArtifact("TestCustomerMapping");
        controller.deleteArtifact("TestOrderMapping1");
        controller.deleteArtifact("TestOrderMapping2");
    }

    @Test
    public void testMappingSettings() throws IOException {
        testHelper.authenticateSession();
        ObjectMapper om = new ObjectMapper();
        controller.updateArtifact("TestCustomerMapping", om.readTree(MAPPING_CONFIG_1));

        JsonNode result = controller.getArtifactSettings("TestCustomerMapping").getBody();
        assertTrue(result.isEmpty(), "No mapping settings yet!");

        JsonNode settings = om.readTree(MAPPING_SETTINGS);

        controller.updateArtifactSettings("TestCustomerMapping", settings);

        result = controller.getArtifactSettings("TestCustomerMapping").getBody();
        assertEquals("TestCustomerMapping", result.get("artifactName").asText());
        assertEquals(2, result.get("additionalCollections").size());
        assertEquals("Collection2", result.get("additionalCollections").get(1).asText());
        assertEquals("data-hub-STAGING", result.get("targetDatabase").asText());
        assertTrue(result.has("permissions"), "missing permissions");
        assertTrue(result.has("customHook"), "missing customHook");

        controller.deleteArtifact("TestCustomerMapping");

        assertTrue(controller.getArtifactSettings("TestCustomerMapping").getBody().isEmpty());
        assertThrows(FailedRequestException.class, () -> controller.getArtifact("TestCustomerMapping"));
    }
}
