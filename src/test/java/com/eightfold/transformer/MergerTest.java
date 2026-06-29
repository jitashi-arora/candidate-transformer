package com.eightfold.transformer;

import com.eightfold.transformer.model.CanonicalCandidate;
import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.model.RawField;
import com.eightfold.transformer.pipeline.Merger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MergerTest {

    private final Merger merger = new Merger();

    private RawCandidate makeCandidate(String source, String name, String email) {
        RawCandidate c = new RawCandidate(source, "test");
        c.addField("fullName", new RawField(name, 0.85, "direct"));
        c.addField("emails", new RawField(List.of(email), 0.85, "direct"));
        return c;
    }

    @Test
    void sameEmailMergesIntoOneCandidate() {
        RawCandidate csv = makeCandidate("recruiter_csv", "Alice Smith", "alice@example.com");
        RawCandidate ats = makeCandidate("ats_json", "Alice S.", "alice@example.com");

        List<CanonicalCandidate> result = merger.merge(List.of(csv, ats));

        assertEquals(1, result.size());
        assertTrue(result.get(0).getEmails().contains("alice@example.com"));
    }

    @Test
    void differentEmailsProduceSeparateCandidates() {
        RawCandidate a = makeCandidate("recruiter_csv", "Alice", "alice@example.com");
        RawCandidate b = makeCandidate("recruiter_csv", "Bob", "bob@example.com");

        List<CanonicalCandidate> result = merger.merge(List.of(a, b));

        assertEquals(2, result.size());
    }

    @Test
    void higherPrioritySourceWinsForName() {
        // github has higher priority than ats_json
        RawCandidate ats = makeCandidate("ats_json", "Alice S.", "alice@example.com");
        RawCandidate github = makeCandidate("github", "Alice Smith", "alice@example.com");

        List<CanonicalCandidate> result = merger.merge(List.of(ats, github));

        assertEquals(1, result.size());
        // github is highest priority, so its name wins
        assertEquals("Alice Smith", result.get(0).getFullName());
    }

    @Test
    void phonesAreNormalizedAndDeduplicated() {
        RawCandidate csv = new RawCandidate("recruiter_csv", "csv");
        csv.addField("emails", new RawField(List.of("alice@example.com"), 0.85, "direct"));
        csv.addField("phones", new RawField(List.of("(408) 867-0001"), 0.85, "direct"));

        RawCandidate notes = new RawCandidate("recruiter_notes", "text");
        notes.addField("emails", new RawField(List.of("alice@example.com"), 0.65, "regex"));
        notes.addField("phones", new RawField(List.of("408-867-0001"), 0.65, "regex"));

        List<CanonicalCandidate> result = merger.merge(List.of(csv, notes));

        // Both phone strings normalize to +14088670001, so deduplicated to 1
        assertEquals(1, result.get(0).getPhones().size());
        assertEquals("+14088670001", result.get(0).getPhones().get(0));
    }

    @Test
    void candidateIdIsStableForSameEmail() {
        RawCandidate c1 = makeCandidate("recruiter_csv", "Alice", "alice@example.com");
        RawCandidate c2 = makeCandidate("recruiter_csv", "Alice", "alice@example.com");

        String id1 = merger.merge(List.of(c1)).get(0).getCandidateId();
        String id2 = merger.merge(List.of(c2)).get(0).getCandidateId();

        assertEquals(id1, id2);
    }
}
