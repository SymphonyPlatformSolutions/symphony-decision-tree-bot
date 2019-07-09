package com.symphony.platformsolutions.decisiontree.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import configuration.SymConfig;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DecisionTreeBotConfig extends SymConfig {
    private String dataFilePath;
    private String adminRoomName;

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
}
