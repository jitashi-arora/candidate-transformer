package com.eightfold.transformer.parser;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.model.RawField;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);
    private static final double CONFIDENCE = 0.85;

    public List<RawCandidate> parse(InputStream inputStream) {
        List<RawCandidate> results = new ArrayList<>();
        if (inputStream == null) return results;

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : records) {
                RawCandidate candidate = new RawCandidate("recruiter_csv", "csv");

                addIfPresent(candidate, "fullName", get(record, "name"));
                addIfPresent(candidate, "experienceCompany", get(record, "current_company"));
                addIfPresent(candidate, "experienceTitle", get(record, "title"));

                String email = get(record, "email");
                List<String> emails = new ArrayList<>();
                if (!email.isBlank()) emails.add(email);
                candidate.addField("emails", new RawField(emails, CONFIDENCE, "direct"));

                String phone = get(record, "phone");
                List<String> phones = new ArrayList<>();
                if (!phone.isBlank()) phones.add(phone);
                candidate.addField("phones", new RawField(phones, CONFIDENCE, "direct"));

                results.add(candidate);
            }
        } catch (IOException e) {
            log.warn("CsvParser: failed to parse CSV - {}", e.getMessage());
        }

        return results;
    }

    private String get(CSVRecord record, String column) {
        try {
            return record.get(column) != null ? record.get(column).trim() : "";
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private void addIfPresent(RawCandidate candidate, String key, String value) {
        if (!value.isBlank()) {
            candidate.addField(key, new RawField(value, CONFIDENCE, "direct"));
        }
    }
}
