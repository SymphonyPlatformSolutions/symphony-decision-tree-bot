package com.symphony.platformsolutions.decisiontree.entity;

import java.util.Objects;

public class Scenario {
    private String question;
    private String answer;

    public Scenario() {}

    public Scenario(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scenario scenario = (Scenario) o;
        return Objects.equals(getQuestion(), scenario.getQuestion()) &&
            Objects.equals(getAnswer(), scenario.getAnswer());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQuestion(), getAnswer());
    }
}
