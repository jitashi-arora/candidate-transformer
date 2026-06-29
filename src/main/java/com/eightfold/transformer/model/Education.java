package com.eightfold.transformer.model;

public class Education {
    private String institution;
    private String degree;
    private String field;
    private Integer endYear;

    public Education() {}

    public String getInstitution() { return institution; }
    public String getDegree() { return degree; }
    public String getField() { return field; }
    public Integer getEndYear() { return endYear; }

    public void setInstitution(String institution) { this.institution = institution; }
    public void setDegree(String degree) { this.degree = degree; }
    public void setField(String field) { this.field = field; }
    public void setEndYear(Integer endYear) { this.endYear = endYear; }
}
