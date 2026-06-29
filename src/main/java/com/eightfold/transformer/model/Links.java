package com.eightfold.transformer.model;

import java.util.ArrayList;
import java.util.List;

public class Links {
    private String linkedin;
    private String github;
    private String portfolio;
    private List<String> other = new ArrayList<>();

    public Links() {}

    public String getLinkedin() { return linkedin; }
    public String getGithub() { return github; }
    public String getPortfolio() { return portfolio; }
    public List<String> getOther() { return other; }

    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    public void setGithub(String github) { this.github = github; }
    public void setPortfolio(String portfolio) { this.portfolio = portfolio; }
    public void setOther(List<String> other) { this.other = other; }
}
