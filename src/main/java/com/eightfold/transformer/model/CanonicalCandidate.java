package com.eightfold.transformer.model;

import java.util.ArrayList;
import java.util.List;

public class CanonicalCandidate {

    private String candidateId;
    private String fullName;
    private List<String> emails = new ArrayList<>();
    private List<String> phones = new ArrayList<>();
    private Location location;
    private Links links = new Links();
    private String headline;
    private Double yearsExperience;
    private List<Skill> skills = new ArrayList<>();
    private List<Experience> experience = new ArrayList<>();
    private List<Education> education = new ArrayList<>();
    private List<ProvenanceEntry> provenance = new ArrayList<>();
    private double overallConfidence;

    public CanonicalCandidate() {}

    public String getCandidateId() { return candidateId; }
    public String getFullName() { return fullName; }
    public List<String> getEmails() { return emails; }
    public List<String> getPhones() { return phones; }
    public Location getLocation() { return location; }
    public Links getLinks() { return links; }
    public String getHeadline() { return headline; }
    public Double getYearsExperience() { return yearsExperience; }
    public List<Skill> getSkills() { return skills; }
    public List<Experience> getExperience() { return experience; }
    public List<Education> getEducation() { return education; }
    public List<ProvenanceEntry> getProvenance() { return provenance; }
    public double getOverallConfidence() { return overallConfidence; }

    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmails(List<String> emails) { this.emails = emails; }
    public void setPhones(List<String> phones) { this.phones = phones; }
    public void setLocation(Location location) { this.location = location; }
    public void setLinks(Links links) { this.links = links; }
    public void setHeadline(String headline) { this.headline = headline; }
    public void setYearsExperience(Double yearsExperience) { this.yearsExperience = yearsExperience; }
    public void setSkills(List<Skill> skills) { this.skills = skills; }
    public void setExperience(List<Experience> experience) { this.experience = experience; }
    public void setEducation(List<Education> education) { this.education = education; }
    public void setProvenance(List<ProvenanceEntry> provenance) { this.provenance = provenance; }
    public void setOverallConfidence(double overallConfidence) { this.overallConfidence = overallConfidence; }
}
