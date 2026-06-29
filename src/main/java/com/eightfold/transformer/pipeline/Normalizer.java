package com.eightfold.transformer.pipeline;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class Normalizer {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[\\w\\-]+\\.[\\w.]+$");

    // Known skill aliases → canonical form
    private static final Map<String, String> SKILL_ALIASES = Map.ofEntries(
            Map.entry("js", "JavaScript"),
            Map.entry("javascript", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("typescript", "TypeScript"),
            Map.entry("k8s", "Kubernetes"),
            Map.entry("kubernetes", "Kubernetes"),
            Map.entry("ml", "Machine Learning"),
            Map.entry("aws", "AWS"),
            Map.entry("gcp", "GCP"),
            Map.entry("sql", "SQL"),
            Map.entry("html", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("api", "API"),
            Map.entry("rest", "REST"),
            Map.entry("ci/cd", "CI/CD"),
            Map.entry("node.js", "Node.js"),
            Map.entry("nodejs", "Node.js"),
            Map.entry("python", "Python"),
            Map.entry("java", "Java")
    );

    // Simple country name → ISO alpha-2 map for common cases
    private static final Map<String, String> COUNTRY_MAP = Map.ofEntries(
            Map.entry("united states", "US"),
            Map.entry("united states of america", "US"),
            Map.entry("usa", "US"),
            Map.entry("us", "US"),
            Map.entry("india", "IN"),
            Map.entry("united kingdom", "GB"),
            Map.entry("uk", "GB"),
            Map.entry("canada", "CA"),
            Map.entry("australia", "AU"),
            Map.entry("germany", "DE"),
            Map.entry("france", "FR"),
            Map.entry("singapore", "SG"),
            Map.entry("netherlands", "NL"),
            Map.entry("japan", "JP"),
            Map.entry("china", "CN")
    );

    private Normalizer() {}

    public static Optional<String> normalizeEmail(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String cleaned = raw.strip().toLowerCase();
        if (EMAIL_PATTERN.matcher(cleaned).matches()) {
            return Optional.of(cleaned);
        }
        return Optional.empty();
    }

    public static Optional<String> normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(raw.strip(), "US");
            if (PHONE_UTIL.isValidNumber(parsed)) {
                return Optional.of(PHONE_UTIL.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164));
            }
        } catch (NumberParseException e) {
            // fall through
        }
        return Optional.empty();
    }

    public static Optional<String> normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String cleaned = raw.strip();

        // Already YYYY-MM
        if (cleaned.matches("\\d{4}-\\d{2}")) return Optional.of(cleaned);

        // Year only → treat as January of that year
        if (cleaned.matches("\\d{4}")) return Optional.of(cleaned + "-01");

        // Try full date formats and extract YYYY-MM
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        };

        for (DateTimeFormatter fmt : formatters) {
            try {
                // Try as YearMonth first
                YearMonth ym = YearMonth.parse(cleaned, fmt);
                return Optional.of(ym.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            } catch (DateTimeParseException ignored) {}
            try {
                LocalDate date = LocalDate.parse(cleaned, fmt);
                return Optional.of(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            } catch (DateTimeParseException ignored) {}
        }

        return Optional.empty();
    }

    public static String canonicalizeSkill(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String trimmed = raw.strip();
        String aliased = SKILL_ALIASES.get(trimmed.toLowerCase());
        if (aliased != null) return aliased;
        // Title-case each word for consistency
        String[] words = trimmed.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() > 0) sb.append(" ");
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
        }
        return sb.isEmpty() ? trimmed : sb.toString();
    }

    public static Optional<String> normalizeCountry(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String key = raw.strip().toLowerCase();
        String result = COUNTRY_MAP.get(key);
        if (result != null) return Optional.of(result);

        // If it's already a 2-letter code, validate and uppercase
        if (raw.strip().length() == 2) {
            String upper = raw.strip().toUpperCase();
            if (COUNTRY_MAP.containsValue(upper)) return Optional.of(upper);
        }

        return Optional.empty();
    }
}
