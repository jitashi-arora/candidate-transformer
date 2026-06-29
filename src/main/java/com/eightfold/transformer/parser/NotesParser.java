package com.eightfold.transformer.parser;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.model.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class NotesParser {

    private static final Logger log = LoggerFactory.getLogger(NotesParser.class);
    private static final double CONFIDENCE = 0.65;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[\\w.+\\-]+@[\\w\\-]+\\.[\\w.]+");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?1[\\s.\\-]?)?\\(?\\d{3}\\)?[\\s.\\-]?\\d{3}[\\s.\\-]?\\d{4}");
    private static final Pattern SKILLS_PATTERN =
            Pattern.compile("(?i)skills\\s*:\\s*(.+)");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^([A-Z][a-z]+( [A-Z][a-z]+)+)");

    public RawCandidate parse(InputStream inputStream, String fileName) {
        if (inputStream == null) return null;

        String text;
        try {
            text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("NotesParser: failed to read file {} - {}", fileName, e.getMessage());
            return null;
        }

        if (text.isBlank()) return null;

        RawCandidate candidate = new RawCandidate("recruiter_notes", "text");

        // Extract emails
        List<String> emails = new ArrayList<>();
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        while (emailMatcher.find()) emails.add(emailMatcher.group());
        candidate.addField("emails", new RawField(emails, CONFIDENCE, "regex"));

        // Extract phones
        List<String> phones = new ArrayList<>();
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) phones.add(phoneMatcher.group().trim());
        candidate.addField("phones", new RawField(phones, CONFIDENCE, "regex"));

        // Extract skills from "Skills: ..." line
        Matcher skillsMatcher = SKILLS_PATTERN.matcher(text);
        if (skillsMatcher.find()) {
            List<String> skills = Arrays.stream(skillsMatcher.group(1).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
            if (!skills.isEmpty()) {
                candidate.addField("skills_raw", new RawField(skills, CONFIDENCE, "regex"));
            }
        }

        // Try to extract name from first line
        String firstLine = text.strip().lines().findFirst().orElse("").trim();
        Matcher nameMatcher = NAME_PATTERN.matcher(firstLine);
        if (nameMatcher.find()) {
            candidate.addField("fullName", new RawField(nameMatcher.group(1), CONFIDENCE, "regex"));
        }

        return candidate;
    }
}
