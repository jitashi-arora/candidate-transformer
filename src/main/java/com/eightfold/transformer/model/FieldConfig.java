package com.eightfold.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FieldConfig {
    private String path;      // output key name
    private String from;      // source path expression, e.g. "emails[0]", "skills[].name"
    private boolean required;

    public FieldConfig() {}

    public String getPath() { return path; }
    public String getFrom() { return from; }
    public boolean isRequired() { return required; }

    public void setPath(String path) { this.path = path; }
    public void setFrom(String from) { this.from = from; }
    public void setRequired(boolean required) { this.required = required; }
}
