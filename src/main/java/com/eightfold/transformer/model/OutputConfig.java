package com.eightfold.transformer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class OutputConfig {

    private List<FieldConfig> fields = new ArrayList<>();

    @JsonProperty("include_confidence")
    private boolean includeConfidence = true;

    @JsonProperty("include_provenance")
    private boolean includeProvenance = true;

    @JsonProperty("on_missing")
    private String onMissing = "null"; // "null", "omit", "error"

    public OutputConfig() {}

    public List<FieldConfig> getFields() { return fields; }
    public boolean isIncludeConfidence() { return includeConfidence; }
    public boolean isIncludeProvenance() { return includeProvenance; }
    public String getOnMissing() { return onMissing; }

    public void setFields(List<FieldConfig> fields) { this.fields = fields; }
    public void setIncludeConfidence(boolean includeConfidence) { this.includeConfidence = includeConfidence; }
    public void setIncludeProvenance(boolean includeProvenance) { this.includeProvenance = includeProvenance; }
    public void setOnMissing(String onMissing) { this.onMissing = onMissing; }
}
