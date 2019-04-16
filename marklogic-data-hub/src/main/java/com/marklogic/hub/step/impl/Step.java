/*
 * Copyright 2012-2019 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.marklogic.hub.step.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.hub.step.StepDefinition;
import com.marklogic.hub.util.json.JSONObject;

import java.util.Map;

public class Step {
    private String name;
    private String description;
    private Map<String, Object> options;
    private JsonNode customHook;
    private int retryLimit;
    private int batchSize;
    private int threadCount;
    private String stepDefinitionName;
    private StepDefinition.StepDefinitionType stepDefinitionType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JsonNode fileLocations;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String modulePath;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    public JsonNode getCustomHook() {
        return customHook;
    }

    public void setCustomHook(JsonNode customHook) {
        this.customHook = customHook;
    }

    public int getRetryLimit() {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit) {
        this.retryLimit = retryLimit;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getStepDefinitionName() {
        return stepDefinitionName;
    }

    public void setStepDefinitionName(String stepDefinitionName) {
        this.stepDefinitionName = stepDefinitionName;
    }

    public StepDefinition.StepDefinitionType getStepDefinitionType() {
        return stepDefinitionType;
    }

    public void setStepDefinitionType(StepDefinition.StepDefinitionType stepDefinitionType) {
        this.stepDefinitionType = stepDefinitionType;
    }

    public JsonNode getFileLocations() {
        return fileLocations;
    }

    public void setFileLocations(JsonNode fileLocations) {
        this.fileLocations = fileLocations;
    }

    public String getModulePath() {
        return modulePath;
    }

    public void setModulePath(String modulePath) {
        this.modulePath = modulePath;
    }

    public static Step deserialize(JsonNode json) {
        Step step = new Step();

        JSONObject jsonObject = new JSONObject(json);
        step.setName(jsonObject.getString("name"));
        step.setDescription(jsonObject.getString("description"));
        step.setOptions(jsonObject.getMap("options"));
        step.setCustomHook(jsonObject.getNode("customHook"));
        step.setRetryLimit(jsonObject.getInt("retryLimit"));
        step.setBatchSize(jsonObject.getInt("batchSize"));
        step.setThreadCount(jsonObject.getInt("threadCount"));
        step.setStepDefinitionName(jsonObject.getString("stepDefinitionName"));
        step.setStepDefinitionType(StepDefinition.StepDefinitionType.getStepDefinitionType(jsonObject.getString("stepDefinitionType")));
        step.setFileLocations(jsonObject.getNode("fileLocations"));

        if (jsonObject.isExist("modulePath")) {
            step.setModulePath(jsonObject.getNode("modulePath").asText());
        }

        return step;
    }
}
