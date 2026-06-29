package com.eightfold.transformer.model;

public class RawField {

    private Object value;
    private double confidence;
    private String method; // "direct", "api", "regex"

    public RawField(Object value, double confidence, String method) {
        this.value = value;
        this.confidence = confidence;
        this.method = method;
    }

    public Object getValue() { return value; }
    public double getConfidence() { return confidence; }
    public String getMethod() { return method; }

    public void setValue(Object value) { this.value = value; }
}
