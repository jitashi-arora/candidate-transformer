package com.eightfold.transformer.model;

public class ProvenanceEntry {
    private String field;
    private String source;
    private String method;

    public ProvenanceEntry(String field, String source, String method) {
        this.field = field;
        this.source = source;
        this.method = method;
    }

    public String getField() { return field; }
    public String getSource() { return source; }
    public String getMethod() { return method; }
}
