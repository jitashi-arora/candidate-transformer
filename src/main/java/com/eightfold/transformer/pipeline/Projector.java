package com.eightfold.transformer.pipeline;

import com.eightfold.transformer.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Projector {

    private final ObjectMapper objectMapper;

    @Autowired
    public Projector(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Apply the OutputConfig to a CanonicalCandidate.
     * If config has no fields defined, return the full canonical profile as a map.
     */
    public Map<String, Object> project(CanonicalCandidate candidate, OutputConfig config) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fullMap = objectMapper.convertValue(candidate, Map.class);

        if (config == null || config.getFields() == null || config.getFields().isEmpty()) {
            if (config != null && !config.isIncludeProvenance()) fullMap.remove("provenance");
            if (config != null && !config.isIncludeConfidence()) {
                fullMap.remove("overall_confidence");
                stripSkillConfidence(fullMap);
            }
            return fullMap;
        }

        Map<String, Object> result = new LinkedHashMap<>();

        for (FieldConfig fc : config.getFields()) {
            String outputKey = fc.getPath();
            String sourcePath = fc.getFrom() != null ? fc.getFrom() : fc.getPath();

            Object value = resolvePath(candidate, sourcePath);

            // Apply per-field normalization if configured
            if (value instanceof String strVal && fc.getNormalize() != null) {
                value = applyNormalization(strVal, fc.getNormalize());
            }

            if (value == null) {
                switch (config.getOnMissing() != null ? config.getOnMissing() : "null") {
                    case "omit" -> {} // don't add key
                    case "error" -> {
                        if (fc.isRequired()) throw new IllegalStateException(
                                "Required field missing: " + outputKey);
                        result.put(outputKey, null);
                    }
                    default -> result.put(outputKey, null); // "null" policy
                }
            } else {
                result.put(outputKey, value);
            }
        }

        if (config.isIncludeConfidence()) result.put("overall_confidence", candidate.getOverallConfidence());
        if (config.isIncludeProvenance()) result.put("provenance", fullMap.get("provenance"));

        return result;
    }

    @SuppressWarnings("unchecked")
    private void stripSkillConfidence(Map<String, Object> fullMap) {
        Object skills = fullMap.get("skills");
        if (!(skills instanceof List<?>)) return;
        for (Object item : (List<?>) skills) {
            if (item instanceof Map<?, ?> skillMap) {
                ((Map<String, Object>) skillMap).remove("confidence");
            }
        }
    }

    private Object applyNormalization(String value, String normalize) {
        return switch (normalize) {
            case "email" -> Normalizer.normalizeEmail(value).orElse(value);
            case "phone" -> Normalizer.normalizePhone(value).orElse(value);
            case "date" -> Normalizer.normalizeDate(value).orElse(value);
            case "country" -> Normalizer.normalizeCountry(value).orElse(value);
            case "skill" -> Normalizer.canonicalizeSkill(value);
            default -> value;
        };
    }

    /**
     * Resolve a path expression against a CanonicalCandidate.
     * Supports:
     *   "emails[0]"       → first element of emails list
     *   "skills[].name"   → list of skill names
     *   "fullName"        → direct field
     */
    private Object resolvePath(CanonicalCandidate c, String path) {
        if (path == null) return null;

        // Pattern: "list[].field" — collect sub-field from each list element
        if (path.contains("[].")) {
            String[] parts = path.split("\\[\\]\\.", 2);
            String listField = parts[0];
            String subField = parts[1];
            List<?> list = getListField(c, listField);
            if (list == null || list.isEmpty()) return null;
            return list.stream()
                    .map(item -> getSubField(item, subField))
                    .filter(v -> v != null)
                    .collect(Collectors.toList());
        }

        // Pattern: "list[0]" — index into a list
        if (path.matches(".*\\[\\d+]$")) {
            int bracket = path.lastIndexOf('[');
            String listField = path.substring(0, bracket);
            int index = Integer.parseInt(path.substring(bracket + 1, path.length() - 1));
            List<?> list = getListField(c, listField);
            if (list == null || list.size() <= index) return null;
            return list.get(index);
        }

        // Direct field access
        return getDirectField(c, path);
    }

    private List<?> getListField(CanonicalCandidate c, String field) {
        return switch (field) {
            case "emails" -> c.getEmails();
            case "phones" -> c.getPhones();
            case "skills" -> c.getSkills();
            case "experience" -> c.getExperience();
            case "education" -> c.getEducation();
            case "provenance" -> c.getProvenance();
            default -> null;
        };
    }

    private Object getSubField(Object item, String field) {
        if (item instanceof Skill skill) {
            return switch (field) {
                case "name" -> skill.getName();
                case "confidence" -> skill.getConfidence();
                default -> null;
            };
        }
        if (item instanceof Experience exp) {
            return switch (field) {
                case "company" -> exp.getCompany();
                case "title" -> exp.getTitle();
                case "start" -> exp.getStart();
                case "end" -> exp.getEnd();
                default -> null;
            };
        }
        if (item instanceof Education edu) {
            return switch (field) {
                case "institution" -> edu.getInstitution();
                case "degree" -> edu.getDegree();
                case "field" -> edu.getField();
                case "end_year", "endYear" -> edu.getEndYear();
                default -> null;
            };
        }
        return null;
    }

    private Object getDirectField(CanonicalCandidate c, String field) {
        return switch (field) {
            case "candidateId", "candidate_id" -> c.getCandidateId();
            case "fullName", "full_name" -> c.getFullName();
            case "emails" -> c.getEmails();
            case "phones" -> c.getPhones();
            case "location" -> c.getLocation();
            case "links" -> c.getLinks();
            case "headline" -> c.getHeadline();
            case "yearsExperience", "years_experience" -> c.getYearsExperience();
            case "skills" -> c.getSkills();
            case "experience" -> c.getExperience();
            case "education" -> c.getEducation();
            case "provenance" -> c.getProvenance();
            case "overallConfidence", "overall_confidence" -> c.getOverallConfidence();
            default -> null;
        };
    }
}
