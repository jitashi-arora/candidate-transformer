package com.eightfold.transformer;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.parser.AtsJsonParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtsJsonParserTest {

    private final AtsJsonParser parser = new AtsJsonParser();

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void mapsForeignFieldsToCanonical() {
        String json = "[{\"candidate_name\": \"Carol Jones\", \"contact_email\": \"carol@corp.com\", \"mobile\": \"+15555550199\"}]";
        List<RawCandidate> result = parser.parse(toStream(json));

        assertEquals(1, result.size());
        assertEquals("Carol Jones", result.get(0).getFields().get("fullName").getValue());
        List<?> emails = (List<?>) result.get(0).getFields().get("emails").getValue();
        assertEquals("carol@corp.com", emails.get(0));
    }

    @Test
    void unknownFieldsAreIgnored() {
        String json = "[{\"candidate_name\": \"Dan\", \"some_weird_field\": \"ignored\"}]";
        List<RawCandidate> result = parser.parse(toStream(json));

        assertNull(result.get(0).getFields().get("some_weird_field"));
    }

    @Test
    void singleObjectRootAlsoWorks() {
        String json = "{\"candidate_name\": \"Eve\", \"contact_email\": \"eve@co.com\"}";
        List<RawCandidate> result = parser.parse(toStream(json));
        assertEquals(1, result.size());
    }

    @Test
    void malformedJsonReturnsEmptyList() {
        String json = "{not valid json";
        List<RawCandidate> result = parser.parse(toStream(json));
        assertTrue(result.isEmpty());
    }

    @Test
    void nullInputReturnsEmptyList() {
        assertTrue(parser.parse(null).isEmpty());
    }
}
