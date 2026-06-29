package com.eightfold.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldConfig {
    private String path;       // output key name
    private String from;       // source path expression, e.g. "emails[0]", "skills[].name"
    private boolean required;
    private String type;       // expected type hint (string, integer, etc.)
    private String normalize;  // normalization to apply: email, phone, date, country, skill

    public FieldConfig() {}

    public String getPath() { return path; }
    public String getFrom() { return from; }
    public boolean isRequired() { return required; }
    public String getType() { return type; }
    public String getNormalize() { return normalize; }

    public void setPath(String path) { this.path = path; }
    public void setFrom(String from) { this.from = from; }
    public void setRequired(boolean required) { this.required = required; }
    public void setType(String type) { this.type = type; }
    public void setNormalize(String normalize) { this.normalize = normalize; }
}
