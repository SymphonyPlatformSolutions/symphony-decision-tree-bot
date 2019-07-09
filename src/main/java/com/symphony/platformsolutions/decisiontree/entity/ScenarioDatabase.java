package com.symphony.platformsolutions.decisiontree.entity;

import java.util.List;

public class ScenarioDatabase {
    private String[] headers;
    private List<ScenarioPath> scenarioPaths;

    public ScenarioDatabase() {}

    public ScenarioDatabase(String[] headers, List<ScenarioPath> scenarioPaths) {
        this.headers = headers;
        this.scenarioPaths = scenarioPaths;
    }

    public String[] getHeaders() {
        return headers;
    }

    public void setHeaders(String[] headers) {
        this.headers = headers;
    }

    public List<ScenarioPath> getScenarioPaths() {
        return scenarioPaths;
    }

    public void setScenarioPaths(List<ScenarioPath> scenarioPaths) {
        this.scenarioPaths = scenarioPaths;
    }
}
