package com.eightfold.transformer.parser;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.model.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubParser {

    private static final Logger log = LoggerFactory.getLogger(GitHubParser.class);
    private static final double CONFIDENCE = 0.88;
    private static final Pattern GITHUB_URL = Pattern.compile(
            "^https?://github\\.com/([A-Za-z0-9_.-]+)/?$"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    public RawCandidate parse(String url) {
        if (url == null || url.isBlank()) return null;

        Matcher matcher = GITHUB_URL.matcher(url.strip());
        if (!matcher.matches()) {
            log.warn("GitHubParser: invalid GitHub URL: {}", url);
            return null;
        }

        String username = matcher.group(1);
        String apiUrl = "https://api.github.com/users/" + username;

        Map<?, ?> data;
        try {
            data = restTemplate.getForObject(apiUrl, Map.class);
        } catch (RestClientException e) {
            log.warn("GitHubParser: API request failed for {} - {}", url, e.getMessage());
            return null;
        }

        if (data == null) return null;

        RawCandidate candidate = new RawCandidate("github", "api");

        Object nameObj = data.get("name") != null ? data.get("name") : data.get("login");
        String name = nameObj != null ? (String) nameObj : null;
        if (name != null && !name.isBlank()) {
            candidate.addField("fullName", new RawField(name, CONFIDENCE, "api"));
        }

        String email = (String) data.get("email");
        List<String> emails = new ArrayList<>();
        if (email != null && !email.isBlank()) emails.add(email);
        candidate.addField("emails", new RawField(emails, CONFIDENCE, "api"));

        String bio = (String) data.get("bio");
        if (bio != null && !bio.isBlank()) {
            candidate.addField("headline", new RawField(bio, CONFIDENCE, "api"));
        }

        String htmlUrl = (String) data.get("html_url");
        if (htmlUrl != null) {
            candidate.addField("links.github", new RawField(htmlUrl, CONFIDENCE, "api"));
        }

        String blog = (String) data.get("blog");
        if (blog != null && !blog.isBlank()) {
            candidate.addField("links.portfolio", new RawField(blog, CONFIDENCE, "api"));
        }

        String company = (String) data.get("company");
        if (company != null && !company.isBlank()) {
            candidate.addField("experienceCompany", new RawField(company.replace("@", "").trim(), CONFIDENCE, "api"));
        }

        String location = (String) data.get("location");
        if (location != null && !location.isBlank()) {
            candidate.addField("location.raw", new RawField(location, CONFIDENCE, "api"));
        }

        return candidate;
    }
}
