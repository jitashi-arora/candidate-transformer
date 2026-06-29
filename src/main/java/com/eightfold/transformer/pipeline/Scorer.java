package com.eightfold.transformer.pipeline;

import com.eightfold.transformer.model.CanonicalCandidate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Scorer {

    public void score(CanonicalCandidate candidate) {
        List<Double> confidences = new ArrayList<>();

        if (candidate.getFullName() != null) confidences.add(0.85);
        if (!candidate.getEmails().isEmpty()) confidences.add(0.90);
        if (!candidate.getPhones().isEmpty()) confidences.add(0.85);
        if (candidate.getHeadline() != null) confidences.add(0.80);
        if (candidate.getLocation() != null) confidences.add(0.75);
        if (!candidate.getExperience().isEmpty()) confidences.add(0.80);
        if (!candidate.getSkills().isEmpty()) confidences.add(0.75);
        if (candidate.getLinks().getGithub() != null) confidences.add(0.88);
        if (candidate.getLinks().getLinkedin() != null) confidences.add(0.85);

        // Boost if multiple sources contributed (more provenance entries = more sources)
        long sourceCount = candidate.getProvenance().stream()
                .map(p -> p.getSource())
                .distinct()
                .filter(s -> !s.equals("merged"))
                .count();
        double boost = sourceCount >= 2 ? 0.05 : 0.0;

        double overall = confidences.isEmpty() ? 0.0
                : confidences.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        overall = Math.min(1.0, overall + boost);
        candidate.setOverallConfidence(Math.round(overall * 100.0) / 100.0);
    }
}
