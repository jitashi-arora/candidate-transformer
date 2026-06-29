package com.eightfold.transformer.model;

import java.util.List;

public class Skill {
    private String name;
    private double confidence;
    private List<String> sources; // which source names contributed this skill

    public Skill() {}

    public Skill(String name, double confidence, List<String> sources) {
        this.name = name;
        this.confidence = confidence;
        this.sources = sources;
    }

    public String getName() { return name; }
    public double getConfidence() { return confidence; }
    public List<String> getSources() { return sources; }

    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setSources(List<String> sources) { this.sources = sources; }
}
