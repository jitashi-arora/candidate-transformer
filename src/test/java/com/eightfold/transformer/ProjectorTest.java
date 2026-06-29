package com.eightfold.transformer;

import com.eightfold.transformer.model.*;
import com.eightfold.transformer.pipeline.Projector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProjectorTest {

    private static ObjectMapper snakeCaseMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return om;
    }

    private final Projector projector = new Projector(snakeCaseMapper());

    private CanonicalCandidate makeCandidate() {
        CanonicalCandidate c = new CanonicalCandidate();
        c.setCandidateId("abc123");
        c.setFullName("Alice Smith");
        c.setEmails(List.of("alice@example.com", "alice@work.com"));
        c.setPhones(List.of("+15555550100"));
        c.setSkills(List.of(
                new Skill("Python", 0.9, List.of("github")),
                new Skill("SQL", 0.8, List.of("csv"))
        ));
        c.setOverallConfidence(0.85);
        return c;
    }

    @Test
    void nullConfigReturnsFullProfile() {
        CanonicalCandidate c = makeCandidate();
        Map<String, Object> result = projector.project(c, null);
        assertEquals("Alice Smith", result.get("full_name"));
    }

    @Test
    void fieldRenameWorks() {
        FieldConfig fc = new FieldConfig();
        fc.setPath("name");
        fc.setFrom("fullName");

        OutputConfig config = new OutputConfig();
        config.setFields(List.of(fc));
        config.setOnMissing("null");

        Map<String, Object> result = projector.project(makeCandidate(), config);
        assertEquals("Alice Smith", result.get("name"));
        assertFalse(result.containsKey("fullName"));
    }

    @Test
    void indexPathExtractsFirstEmail() {
        FieldConfig fc = new FieldConfig();
        fc.setPath("primary_email");
        fc.setFrom("emails[0]");

        OutputConfig config = new OutputConfig();
        config.setFields(List.of(fc));
        config.setOnMissing("null");

        Map<String, Object> result = projector.project(makeCandidate(), config);
        assertEquals("alice@example.com", result.get("primary_email"));
    }

    @Test
    void listSubFieldExtractsSkillNames() {
        FieldConfig fc = new FieldConfig();
        fc.setPath("skill_names");
        fc.setFrom("skills[].name");

        OutputConfig config = new OutputConfig();
        config.setFields(List.of(fc));
        config.setOnMissing("null");

        Map<String, Object> result = projector.project(makeCandidate(), config);
        List<?> names = (List<?>) result.get("skill_names");
        assertTrue(names.contains("Python"));
        assertTrue(names.contains("SQL"));
    }

    @Test
    void onMissingOmitSkipsNullFields() {
        FieldConfig fc = new FieldConfig();
        fc.setPath("yearsExperience");
        fc.setFrom("years_experience");

        OutputConfig config = new OutputConfig();
        config.setFields(List.of(fc));
        config.setOnMissing("omit");

        Map<String, Object> result = projector.project(makeCandidate(), config);
        assertFalse(result.containsKey("yearsExperience"));
    }
}
