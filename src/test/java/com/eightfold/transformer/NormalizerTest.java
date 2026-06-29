package com.eightfold.transformer;

import com.eightfold.transformer.pipeline.Normalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizerTest {

    // Email tests
    @Test
    void email_lowercasesAndStrips() {
        assertEquals("alice@example.com", Normalizer.normalizeEmail("  Alice@EXAMPLE.COM  ").orElse(null));
    }

    @Test
    void email_invalidReturnsEmpty() {
        assertTrue(Normalizer.normalizeEmail("not-an-email").isEmpty());
    }

    @Test
    void email_nullReturnsEmpty() {
        assertTrue(Normalizer.normalizeEmail(null).isEmpty());
    }

    // Phone tests
    @Test
    void phone_formattedToE164() {
        // (408) 867-0001 — valid NANP: area 408 (San Jose), NXX 867, subscriber 0001
        assertEquals("+14088670001", Normalizer.normalizePhone("(408) 867-0001").orElse(null));
    }

    @Test
    void phone_alreadyE164Passthrough() {
        assertEquals("+14088670001", Normalizer.normalizePhone("+14088670001").orElse(null));
    }

    @Test
    void phone_invalidReturnsEmpty() {
        assertTrue(Normalizer.normalizePhone("not-a-phone").isEmpty());
    }

    @Test
    void phone_nullReturnsEmpty() {
        assertTrue(Normalizer.normalizePhone(null).isEmpty());
    }

    // Date tests
    @Test
    void date_alreadyYYYYMM() {
        assertEquals("2022-03", Normalizer.normalizeDate("2022-03").orElse(null));
    }

    @Test
    void date_yearOnly() {
        assertEquals("2022-01", Normalizer.normalizeDate("2022").orElse(null));
    }

    @Test
    void date_monthYear() {
        assertEquals("2022-03", Normalizer.normalizeDate("March 2022").orElse(null));
    }

    @Test
    void date_garbageReturnsEmpty() {
        assertTrue(Normalizer.normalizeDate("whenever").isEmpty());
    }

    // Country tests
    @Test
    void country_fullNameToAlpha2() {
        assertEquals("US", Normalizer.normalizeCountry("United States").orElse(null));
    }

    @Test
    void country_alpha2Passthrough() {
        assertEquals("US", Normalizer.normalizeCountry("US").orElse(null));
    }

    @Test
    void country_unknownReturnsEmpty() {
        assertTrue(Normalizer.normalizeCountry("Narnia").isEmpty());
    }
}
