package com.eightfold.transformer.parser;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.model.RawField;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AtsJsonParser {

    private static final Logger log = LoggerFactory.getLogger(AtsJsonParser.class);
    private static final double CONFIDENCE = 0.80;

    // Maps ATS foreign field names → canonical RawCandidate field keys
    private static final Map<String, String> FIELD_MAP = Map.ofEntries(
            Map.entry("candidate_name", "fullName"),
            Map.entry("contact_email", "emails"),
            Map.entry("email_address", "emails"),
            Map.entry("mobile", "phones"),
            Map.entry("phone_number", "phones"),
            Map.entry("current_role", "experienceTitle"),
            Map.entry("position_title", "experienceTitle"),
            Map.entry("current_company", "experienceCompany"),
            Map.entry("linkedin_url", "links.linkedin"),
            Map.entry("github_url", "links.github"),
            Map.entry("location_city", "location.city"),
            Map.entry("location_country", "location.country"),
            Map.entry("institution", "education.institution"),
            Map.entry("degree", "education.degree"),
            Map.entry("field_of_study", "education.field"),
            Map.entry("graduation_year", "education.endYear")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RawCandidate> parse(InputStream inputStream) {
        List<RawCandidate> results = new ArrayList<>();
        if (inputStream == null) return results;

        List<Map<String, Object>> records;
        try {
            Object raw = objectMapper.readValue(inputStream, Object.class);
            if (raw instanceof List<?>) {
                records = objectMapper.convertValue(raw, new TypeReference<List<Map<String, Object>>>() {});
            } else if (raw instanceof Map<?, ?>) {
                Map<String, Object> single = objectMapper.convertValue(raw, new TypeReference<Map<String, Object>>() {});
                records = List.of(single);
            } else {
                log.warn("AtsJsonParser: unexpected root type");
                return results;
            }
        } catch (IOException e) {
            log.warn("AtsJsonParser: failed to parse JSON - {}", e.getMessage());
            return results;
        }

        for (Map<String, Object> record : records) {
            RawCandidate candidate = new RawCandidate("ats_json", "json");

            for (Map.Entry<String, Object> entry : record.entrySet()) {
                String canonicalKey = FIELD_MAP.get(entry.getKey());
                if (canonicalKey == null) continue; // unknown field: skip

                Object value = entry.getValue();
                if (value == null) continue;

                // Wrap single string email/phone into a list
                if ((canonicalKey.equals("emails") || canonicalKey.equals("phones")) && value instanceof String) {
                    List<String> list = new ArrayList<>();
                    String str = ((String) value).trim();
                    if (!str.isBlank()) list.add(str);
                    candidate.addField(canonicalKey, new RawField(list, CONFIDENCE, "direct"));
                } else {
                    candidate.addField(canonicalKey, new RawField(value, CONFIDENCE, "direct"));
                }
            }

            results.add(candidate);
        }

        return results;
    }
}
