package com.eightfold.transformer;

import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.parser.CsvParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserTest {

    private final CsvParser parser = new CsvParser();

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesBasicRow() {
        String csv = "name,email,phone,current_company,title\n" +
                     "Alice Smith,alice@example.com,+15555550100,Acme Corp,Engineer";
        List<RawCandidate> result = parser.parse(toStream(csv));

        assertEquals(1, result.size());
        assertEquals("Alice Smith", result.get(0).getFields().get("fullName").getValue());
        List<?> emails = (List<?>) result.get(0).getFields().get("emails").getValue();
        assertEquals("alice@example.com", emails.get(0));
    }

    @Test
    void emptyPhoneProducesEmptyList() {
        String csv = "name,email,phone,current_company,title\n" +
                     "Bob,,,,";
        List<RawCandidate> result = parser.parse(toStream(csv));

        List<?> phones = (List<?>) result.get(0).getFields().get("phones").getValue();
        assertTrue(phones.isEmpty());
    }

    @Test
    void emptyFileReturnsEmptyList() {
        String csv = "name,email,phone,current_company,title\n";
        List<RawCandidate> result = parser.parse(toStream(csv));
        assertTrue(result.isEmpty());
    }

    @Test
    void nullInputReturnsEmptyList() {
        List<RawCandidate> result = parser.parse(null);
        assertTrue(result.isEmpty());
    }
}
