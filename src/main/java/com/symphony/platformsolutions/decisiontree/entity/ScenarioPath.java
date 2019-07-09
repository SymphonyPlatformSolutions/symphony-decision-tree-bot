package com.symphony.platformsolutions.decisiontree.entity;

import java.util.LinkedList;
import java.util.List;

public class ScenarioPath {
    private List<Scenario> scenarios;

    public ScenarioPath() {
        this.scenarios = new LinkedList<>();
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }

    public void addScenario(Scenario scenario) {
        this.scenarios.add(scenario);
    }
}
