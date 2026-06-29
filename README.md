# Candidate Data Transformer

A Spring Boot web application that ingests candidate data from multiple sources and produces one clean, canonical JSON profile per candidate — with provenance tracking and confidence scoring.

## What it does

- Accepts up to 4 source types per run: **Recruiter CSV**, **ATS JSON**, **Recruiter Notes (.txt)**, and a **GitHub profile URL**
- Merges records for the same candidate (matched by email) across all sources
- Normalises phones to E.164, emails to lowercase, dates to YYYY-MM, countries to ISO-3166 alpha-2
- Assigns a confidence score to each field and an overall score per candidate
- Tracks provenance: every field records which source it came from and how it was extracted
- Accepts a **runtime JSON config** to reshape the output (select fields, rename them, toggle provenance, set on-missing policy)

## Sources implemented

| Source | Type | Notes |
|---|---|---|
| Recruiter CSV | Structured | Columns: `name, email, phone, current_company, title` |
| ATS JSON | Structured | Foreign field names mapped to canonical (see `AtsJsonParser.java`) |
| GitHub URL | Unstructured | Public REST API — no token needed |
| Recruiter notes (.txt) | Unstructured | Regex extraction of email, phone, skills, name |

**Not implemented:** LinkedIn (requires OAuth), PDF/DOCX resume (out of scope).

## Requirements

- Java 21+
- Maven 3.8+

## Build

```bash
# If your default Maven settings route through a corporate proxy, use the bundled settings file:
JAVA_HOME=~/.sdkman/candidates/java/21.0.3-amzn \
  mvn clean package -s mvn-settings.xml -DskipTests

# Standard build (no proxy issues):
mvn clean package -DskipTests
```

The JAR is created at `target/candidate-transformer-0.1.0.jar`.

## Run

```bash
java -jar target/candidate-transformer-0.1.0.jar
```

Open **http://localhost:8080** in your browser.

## Use the UI

1. Upload any combination of source files from the `samples/` folder
2. Optionally paste a config JSON (or leave the default)
3. Click **Transform** to see the merged profiles

## Run tests

```bash
mvn test -s mvn-settings.xml       # with proxy settings
mvn test                           # standard
```

33 unit tests covering: normalization, CSV parser, ATS JSON parser, merge logic, and the projection layer.

## Output config

Paste a JSON config into the textarea to reshape the output. Leave blank for full canonical output.

**Default (full output):**
```json
{
  "include_confidence": true,
  "include_provenance": true,
  "on_missing": "null"
}
```

**Custom (select + rename fields):**
```json
{
  "fields": [
    {"path": "full_name", "from": "fullName", "required": true},
    {"path": "primary_email", "from": "emails[0]", "required": true},
    {"path": "phone", "from": "phones[0]"},
    {"path": "skill_names", "from": "skills[].name"}
  ],
  "include_confidence": true,
  "include_provenance": false,
  "on_missing": "null"
}
```

**Supported path expressions:**
- `"fullName"` — direct field
- `"emails[0]"` — first element of a list
- `"skills[].name"` — extract sub-field from every list element

**`on_missing` values:** `"null"` (output null), `"omit"` (skip key), `"error"` (throw if required).

## Sample files

| File | Description |
|---|---|
| `samples/candidates.csv` | 3 candidates — Alice, Bob, Carol |
| `samples/ats_data.json` | ATS records for Alice + Bob with foreign field names |
| `samples/notes_alice.txt` | Free-text recruiter notes for Alice |
| `samples/notes_bob.txt` | Free-text recruiter notes for Bob |
| `configs/default.json` | Default config (full output) |
| `configs/custom.json` | Custom config matching the assignment example |

## Pipeline overview

```
Upload → Detect source type → Parse → Normalise → Merge by email identity
       → Score confidence → Project (apply config) → Render
```

Each stage is a separate class (`pipeline/`). A parser failure logs a warning and returns an empty list — the pipeline never crashes on a bad source.

## Key design decisions

1. **Null over guess** — if a phone can't be normalised to E.164, it's stored as null, not the raw string. A downstream system can trust every value it sees.

2. **Clean separation** — the core pipeline always builds a full `CanonicalCandidate`. The `Projector` reads the config and builds a new `Map<String,Object>` — the canonical record is never mutated.

3. **Deterministic ID** — `candidateId` is `SHA-256(primaryEmail)[:16]`, so the same candidate always gets the same ID regardless of source order.

## Assumptions noted

- Phone numbers in sample data are fictitious (structurally valid NANP format, not real numbers).
- GitHub public API is used without authentication — sufficient for demo (60 req/hr limit).
- Identity matching uses the primary email only. Fuzzy name matching was excluded to avoid false positives.
