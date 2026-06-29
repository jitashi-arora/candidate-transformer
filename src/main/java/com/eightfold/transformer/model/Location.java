package com.eightfold.transformer.model;

public class Location {
    private String city;
    private String region;
    private String country; // ISO-3166 alpha-2

    public Location() {}

    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }

    public void setCity(String city) { this.city = city; }
    public void setRegion(String region) { this.region = region; }
    public void setCountry(String country) { this.country = country; }
}
