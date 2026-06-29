package com.eightfold.transformer.pipeline;

import com.eightfold.transformer.model.CanonicalCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Validator {

    private static final Logger log = LoggerFactory.getLogger(Validator.class);

    /**
     * Validate a CanonicalCandidate before it is projected and returned.
     * Returns a list of issue strings — empty list means the candidate is valid.
     * Issues are also logged as warnings.
     */
    public List<String> validate(CanonicalCandidate candidate) {
        List<String> issues = new ArrayList<>();

        if (candidate.getCandidateId() == null || candidate.getCandidateId().isBlank()) {
            issues.add("candidate_id is missing");
        }
        if (candidate.getEmails() == null || candidate.getEmails().isEmpty()) {
            issues.add("no emails found — identity matching may be unreliable");
        }
        if (candidate.getFullName() == null || candidate.getFullName().isBlank()) {
            issues.add("full_name is missing");
        }
        if (candidate.getOverallConfidence() < 0.0 || candidate.getOverallConfidence() > 1.0) {
            issues.add("overall_confidence out of range: " + candidate.getOverallConfidence());
        }

        if (!issues.isEmpty()) {
            log.warn("Validation warnings for candidate [{}]: {}",
                    candidate.getCandidateId() != null ? candidate.getCandidateId() : "unknown",
                    String.join("; ", issues));
        }

        return issues;
    }
}
