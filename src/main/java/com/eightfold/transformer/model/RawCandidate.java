package com.eightfold.transformer.model;

import java.util.HashMap;
import java.util.Map;

public class RawCandidate {

    private String sourceName; // "recruiter_csv", "ats_json", "github", "recruiter_notes"
    private String sourceType; // "csv", "json", "api", "text"
    private Map<String, RawField> fields;

    public RawCandidate(String sourceName, String sourceType) {
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.fields = new HashMap<>();
    }

    public void addField(String key, RawField field) {
        fields.put(key, field);
    }

    public String getSourceName() { return sourceName; }
    public String getSourceType() { return sourceType; }
    public Map<String, RawField> getFields() { return fields; }
}
