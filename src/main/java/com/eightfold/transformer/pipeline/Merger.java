package com.eightfold.transformer.pipeline;

import com.eightfold.transformer.model.*;
import com.eightfold.transformer.util.CandidateIdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Merger {

    private static final Logger log = LoggerFactory.getLogger(Merger.class);

    // Higher index = higher priority
    private static final List<String> SOURCE_PRIORITY =
            List.of("recruiter_notes", "ats_json", "recruiter_csv", "github");

    public List<CanonicalCandidate> merge(List<RawCandidate> rawCandidates) {
        // Group records by normalized primary email
        Map<String, List<RawCandidate>> groups = groupByEmail(rawCandidates);

        List<CanonicalCandidate> results = new ArrayList<>();
        for (Map.Entry<String, List<RawCandidate>> entry : groups.entrySet()) {
            results.add(buildCanonical(entry.getKey(), entry.getValue()));
        }
        return results;
    }

    private Map<String, List<RawCandidate>> groupByEmail(List<RawCandidate> candidates) {
        Map<String, List<RawCandidate>> groups = new LinkedHashMap<>();

        for (RawCandidate candidate : candidates) {
            String groupKey = getPrimaryEmail(candidate);
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(candidate);
        }

        return groups;
    }

    private String getPrimaryEmail(RawCandidate candidate) {
        RawField emailField = candidate.getFields().get("emails");
        if (emailField != null && emailField.getValue() instanceof List<?> list && !list.isEmpty()) {
            String email = list.get(0).toString().toLowerCase().strip();
            Optional<String> normalized = Normalizer.normalizeEmail(email);
            if (normalized.isPresent()) return normalized.get();
        }
        // No valid email — use a unique key so the record gets its own group
        return "no-email-" + UUID.randomUUID();
    }

    private CanonicalCandidate buildCanonical(String groupKey, List<RawCandidate> group) {
        // Sort group so highest-priority source is last (we overwrite with higher priority)
        group.sort(Comparator.comparingInt(r -> SOURCE_PRIORITY.indexOf(r.getSourceName())));

        CanonicalCandidate canonical = new CanonicalCandidate();
        List<ProvenanceEntry> provenance = new ArrayList<>();

        // Collect emails across all sources (union)
        Set<String> emailSet = new LinkedHashSet<>();
        Set<String> phoneSet = new LinkedHashSet<>();
        Map<String, Skill> skillMap = new LinkedHashMap<>();

        for (RawCandidate raw : group) {
            Map<String, RawField> fields = raw.getFields();

            setIfBetter(canonical, "fullName", raw, fields.get("fullName"), provenance);
            setIfBetter(canonical, "headline", raw, fields.get("headline"), provenance);
            setIfBetter(canonical, "yearsExperience", raw, fields.get("yearsExperience"), provenance);

            collectEmails(fields.get("emails"), emailSet);
            collectPhones(fields.get("phones"), phoneSet);
            collectSkills(fields.get("skills_raw"), raw.getSourceName(), skillMap);

            buildExperience(canonical, fields, raw.getSourceName(), provenance);
            buildEducation(canonical, fields, raw.getSourceName(), provenance);
            buildLinks(canonical, fields, raw.getSourceName(), provenance);
            buildLocation(canonical, fields, raw.getSourceName(), provenance);
        }

        // Set identity
        boolean hasEmail = !emailSet.isEmpty();
        canonical.setEmails(new ArrayList<>(emailSet));
        canonical.setPhones(new ArrayList<>(phoneSet));
        canonical.setSkills(new ArrayList<>(skillMap.values()));

        String idInput = hasEmail ? emailSet.iterator().next() : canonical.getFullName();
        canonical.setCandidateId(CandidateIdUtil.generate(idInput));

        if (!emailSet.isEmpty()) {
            provenance.add(new ProvenanceEntry("emails", "merged", "union"));
        }
        if (!phoneSet.isEmpty()) {
            provenance.add(new ProvenanceEntry("phones", "merged", "union"));
        }

        canonical.setProvenance(provenance);
        return canonical;
    }

    private void setIfBetter(CanonicalCandidate canonical, String fieldName,
                              RawCandidate raw, RawField field,
                              List<ProvenanceEntry> provenance) {
        if (field == null || field.getValue() == null) return;
        String value = field.getValue().toString();
        if (value.isBlank()) return;

        switch (fieldName) {
            case "fullName" -> {
                canonical.setFullName(value);
                provenance.add(new ProvenanceEntry("fullName", raw.getSourceName(), field.getMethod()));
            }
            case "headline" -> {
                canonical.setHeadline(value);
                provenance.add(new ProvenanceEntry("headline", raw.getSourceName(), field.getMethod()));
            }
            case "yearsExperience" -> {
                try {
                    canonical.setYearsExperience(Double.parseDouble(value));
                    provenance.add(new ProvenanceEntry("years_experience", raw.getSourceName(), field.getMethod()));
                } catch (NumberFormatException e) {
                    log.warn("Merger: could not parse yearsExperience '{}' from {}", value, raw.getSourceName());
                }
            }
        }
    }

    private void collectEmails(RawField field, Set<String> emailSet) {
        if (field == null || !(field.getValue() instanceof List<?>)) return;
        for (Object item : (List<?>) field.getValue()) {
            String email = item.toString().strip();
            Normalizer.normalizeEmail(email).ifPresent(emailSet::add);
        }
    }

    private void collectPhones(RawField field, Set<String> phoneSet) {
        if (field == null || !(field.getValue() instanceof List<?>)) return;
        for (Object item : (List<?>) field.getValue()) {
            String phone = item.toString().strip();
            Normalizer.normalizePhone(phone).ifPresent(phoneSet::add);
        }
    }

    private void collectSkills(RawField field, String sourceName, Map<String, Skill> skillMap) {
        if (field == null || !(field.getValue() instanceof List<?>)) return;
        for (Object item : (List<?>) field.getValue()) {
            String skillName = Normalizer.canonicalizeSkill(item.toString().trim());
            String key = skillName.toLowerCase();
            if (skillMap.containsKey(key)) {
                // Already seen — add source to list
                Skill existing = skillMap.get(key);
                if (!existing.getSources().contains(sourceName)) {
                    existing.getSources().add(sourceName);
                    existing.setConfidence(Math.min(1.0, existing.getConfidence() + 0.05));
                }
            } else {
                skillMap.put(key, new Skill(skillName, field.getConfidence(), new ArrayList<>(List.of(sourceName))));
            }
        }
    }

    private void buildExperience(CanonicalCandidate canonical, Map<String, RawField> fields,
                                  String sourceName, List<ProvenanceEntry> provenance) {
        RawField companyField = fields.get("experienceCompany");
        RawField titleField = fields.get("experienceTitle");
        if (companyField == null && titleField == null) return;

        String company = companyField != null ? companyField.getValue().toString() : "";
        String title = titleField != null ? titleField.getValue().toString() : "";

        // Deduplicate by company — same company from multiple sources is the same role.
        // We process sources in ascending priority order, so the last write (highest priority) wins.
        List<Experience> existing = canonical.getExperience();
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).getCompany().equalsIgnoreCase(company)) {
                existing.set(i, new Experience(company, title)); // replace with higher-priority title
                return;
            }
        }

        existing.add(new Experience(company, title));
        provenance.add(new ProvenanceEntry("experience", sourceName, "direct"));
    }

    private void buildEducation(CanonicalCandidate canonical, Map<String, RawField> fields,
                                String sourceName, List<ProvenanceEntry> provenance) {
        RawField instField = fields.get("education.institution");
        RawField degreeField = fields.get("education.degree");
        RawField fieldField = fields.get("education.field");
        RawField yearField = fields.get("education.endYear");

        if (instField == null && degreeField == null) return;

        String institution = instField != null ? instField.getValue().toString() : null;

        // Deduplicate by institution name
        List<Education> existing = canonical.getEducation();
        boolean alreadyPresent = existing.stream().anyMatch(e ->
                institution != null && institution.equalsIgnoreCase(e.getInstitution()));
        if (alreadyPresent) return;

        Education edu = new Education();
        edu.setInstitution(institution);
        if (degreeField != null) edu.setDegree(degreeField.getValue().toString());
        if (fieldField != null) edu.setField(fieldField.getValue().toString());
        if (yearField != null) {
            try {
                edu.setEndYear(Integer.parseInt(yearField.getValue().toString()));
            } catch (NumberFormatException ignored) {}
        }

        existing.add(edu);
        provenance.add(new ProvenanceEntry("education", sourceName, "direct"));
    }

    private void buildLinks(CanonicalCandidate canonical, Map<String, RawField> fields,
                             String sourceName, List<ProvenanceEntry> provenance) {
        Links links = canonical.getLinks();

        RawField linkedin = fields.get("links.linkedin");
        if (linkedin != null && links.getLinkedin() == null) {
            links.setLinkedin(linkedin.getValue().toString());
            provenance.add(new ProvenanceEntry("links.linkedin", sourceName, linkedin.getMethod()));
        }

        RawField github = fields.get("links.github");
        if (github != null && links.getGithub() == null) {
            links.setGithub(github.getValue().toString());
            provenance.add(new ProvenanceEntry("links.github", sourceName, github.getMethod()));
        }

        RawField portfolio = fields.get("links.portfolio");
        if (portfolio != null && links.getPortfolio() == null) {
            links.setPortfolio(portfolio.getValue().toString());
            provenance.add(new ProvenanceEntry("links.portfolio", sourceName, portfolio.getMethod()));
        }
    }

    private void buildLocation(CanonicalCandidate canonical, Map<String, RawField> fields,
                                String sourceName, List<ProvenanceEntry> provenance) {
        RawField cityField = fields.get("location.city");
        RawField countryField = fields.get("location.country");
        RawField rawField = fields.get("location.raw");

        if (cityField == null && countryField == null && rawField == null) return;

        Location location = canonical.getLocation() != null ? canonical.getLocation() : new Location();

        if (cityField != null && location.getCity() == null) {
            location.setCity(cityField.getValue().toString());
        }
        if (countryField != null && location.getCountry() == null) {
            String country = countryField.getValue().toString();
            Normalizer.normalizeCountry(country).ifPresentOrElse(
                    location::setCountry,
                    () -> location.setCountry(country)
            );
        }
        if (rawField != null && location.getCity() == null) {
            // Parse free-text location like "San Francisco, CA, US"
            String[] parts = rawField.getValue().toString().split(",");
            if (parts.length > 0) location.setCity(parts[0].trim());
            if (parts.length > 1) location.setRegion(parts[1].trim());
            if (parts.length > 2) {
                Normalizer.normalizeCountry(parts[2].trim())
                        .ifPresent(location::setCountry);
            }
        }

        canonical.setLocation(location);
        provenance.add(new ProvenanceEntry("location", sourceName, "direct"));
    }
}
