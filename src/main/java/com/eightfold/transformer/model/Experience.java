package com.eightfold.transformer.model;

public class Experience {
    private String company;
    private String title;
    private String start;   // YYYY-MM
    private String end;     // YYYY-MM or null = present
    private String summary;

    public Experience() {}

    public Experience(String company, String title) {
        this.company = company;
        this.title = title;
    }

    public String getCompany() { return company; }
    public String getTitle() { return title; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getSummary() { return summary; }

    public void setCompany(String company) { this.company = company; }
    public void setTitle(String title) { this.title = title; }
    public void setStart(String start) { this.start = start; }
    public void setEnd(String end) { this.end = end; }
    public void setSummary(String summary) { this.summary = summary; }
}
