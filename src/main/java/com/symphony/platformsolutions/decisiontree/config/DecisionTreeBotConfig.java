package com.symphony.platformsolutions.decisiontree.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import configuration.SymConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DecisionTreeBotConfig extends SymConfig {
    private String dataFilePath;
    private String adminRoomName;
    private boolean healthCheckEnabled;
    private String welcomeMessage;
    private String invalidChoiceMessage;
    private String completionMessage;

    public String getDataFilePath() {
        return dataFilePath;
    }

    public void setDataFilePath(String dataFilePath) {
        this.dataFilePath = dataFilePath;
    }

    public String getAdminRoomName() {
        return adminRoomName;
    }

    public void setAdminRoomName(String adminRoomName) {
        this.adminRoomName = adminRoomName;
    }

    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled;
    }

    public void setHealthCheckEnabled(boolean healthCheckEnabled) {
        this.healthCheckEnabled = healthCheckEnabled;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getInvalidChoiceMessage() {
        return invalidChoiceMessage;
    }

    public void setInvalidChoiceMessage(String invalidChoiceMessage) {
        this.invalidChoiceMessage = invalidChoiceMessage;
    }

    public String getCompletionMessage() {
        return completionMessage;
    }

    public void setCompletionMessage(String completionMessage) {
        this.completionMessage = completionMessage;
    }
}
