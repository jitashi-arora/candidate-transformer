# Multi-Source Candidate Data Transformer

A Spring Boot web application that ingests candidate data from multiple heterogeneous sources and produces one clean, canonical JSON profile per candidate — with confidence scoring, provenance tracking, and a runtime-configurable output shape.

Pipeline: **parse → normalize → merge → score → validate → project → render**

---

## What It Does

- Accepts up to four source types per run: a recruiter CSV, an ATS JSON export, a GitHub profile URL, and a recruiter notes text file.
- Matches records for the same candidate across sources using their normalized primary email.
- Normalizes phones to E.164, emails to lowercase, dates to `YYYY-MM`, and countries to ISO-3166 alpha-2.
- Assigns a confidence score to every field and an overall score per candidate, boosted when multiple sources agree.
- Records provenance for every field: which source it came from and how it was extracted.
- Accepts a runtime JSON config in the UI to reshape the output — select fields, rename them, set missing-value policy, toggle provenance and confidence.

---

## Tech Stack

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.3.0 |
| Build tool | Maven 3.8+ |
| Templating | Thymeleaf + Bootstrap 5 (CDN) |
| CSV parsing | Apache Commons CSV 1.11.0 |
| Phone normalization | Google libphonenumber 8.13.39 |
| JSON | Jackson (bundled with Spring Boot) |

---

## Prerequisites

### Java 21

You need Java 21 on your machine. Choose either option below.

**Option A — SDKMAN (recommended on macOS/Linux, easiest way to manage multiple Java versions)**

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 21
sdk install java 21.0.3-amzn

# Verify
java -version
# Expected: openjdk version "21.0.3" ...
```

**Option B — Direct download (works on macOS, Linux, Windows)**

1. Go to [https://adoptium.net/temurin/releases/](https://adoptium.net/temurin/releases/)
2. Select **Version 21**, your OS, and your architecture (x64 or aarch64/ARM).
3. Download and run the installer.
4. After installation, verify:

```bash
java -version
# Expected: openjdk version "21.x.x" ...
```

If `java -version` still shows an older version after installation, set `JAVA_HOME` manually:

```bash
# macOS / Linux — add this to your ~/.zshrc or ~/.bashrc
export JAVA_HOME=/path/to/your/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

On Windows, set `JAVA_HOME` in System Properties > Environment Variables.

### Maven 3.8+

Maven is the build tool. Check if it is already installed:

```bash
mvn -version
```

If not installed:
- **macOS (Homebrew):** `brew install maven`
- **Linux (apt):** `sudo apt install maven`
- **Windows / manual:** Download from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi) and add `bin/` to your `PATH`.

---

## Build

Clone the repository and build the JAR:

```bash
git clone https://github.com/jitashi-arora/candidate-transformer.git
cd candidate-transformer
mvn clean package -DskipTests
```

The JAR is created at `target/candidate-transformer-0.1.0.jar`.

**If you are behind a corporate Maven proxy (e.g. Nexus/Artifactory) that blocks certain dependencies**, use the bundled settings file which routes directly to Maven Central:

```bash
mvn clean package -s mvn-settings.xml -DskipTests
```

---

## Run

```bash
java -jar target/candidate-transformer-0.1.0.jar
```

Open **http://localhost:8080** in your browser.

---

## Using the UI

1. **Upload source files** — use the file pickers on the form to upload any combination of:
   - A recruiter CSV (`samples/candidates.csv`)
   - An ATS JSON export (`samples/ats_data.json`)
   - A recruiter notes text file (`samples/notes_alice.txt` or `samples/notes_bob.txt`)
2. **Paste a GitHub profile URL** (optional) — for example `https://github.com/torvalds`. Only the public profile is read; no token is required.
3. **Paste an output config** — the textarea is pre-filled with the default passthrough config. Replace it with a custom config (see the [Output Config](#output-config) section) to reshape the output.
4. Click **Transform**.
5. The result page shows one JSON block per candidate. Use the **Download JSON** button to save the full array.

---

## Input Sources

### Recruiter CSV

Expected column headers (order does not matter, extra columns are ignored):

| Column | Description |
|---|---|
| `name` | Candidate full name |
| `email` | Primary email address |
| `phone` | Phone number (any common format) |
| `current_company` | Current employer |
| `title` | Current job title |

Example row:

```
name,email,phone,current_company,title
Alice Smith,alice@example.com,+14088670001,Acme Corp,Senior Engineer
```

See `samples/candidates.csv` for a ready-to-use file.

---

### ATS JSON

A JSON array of candidate objects. Field names do not need to match the canonical schema — the parser maps them automatically using the table below.

| ATS field name | Canonical field |
|---|---|
| `candidate_name` | `full_name` |
| `contact_email` or `email_address` | `emails` |
| `mobile` or `phone_number` | `phones` |
| `current_role` or `position_title` | `experience[].title` |
| `current_company` | `experience[].company` |
| `linkedin_url` | `links.linkedin` |
| `github_url` | `links.github` |
| `location_city` | `location.city` |
| `location_country` | `location.country` |
| `institution` | `education[].institution` |
| `degree` | `education[].degree` |
| `field_of_study` | `education[].field` |
| `graduation_year` | `education[].end_year` |

Any unrecognized key is silently skipped.

Example object:

```json
{
  "candidate_name": "Alice Smith",
  "contact_email": "alice@example.com",
  "mobile": "+1 408-867-0001",
  "current_role": "Senior Software Engineer",
  "current_company": "Acme Corp",
  "linkedin_url": "https://linkedin.com/in/alicesmith",
  "location_city": "San Francisco",
  "location_country": "United States",
  "institution": "MIT",
  "degree": "B.S. Computer Science",
  "graduation_year": 2019
}
```

See `samples/ats_data.json` for a ready-to-use file with two candidates.

---

### GitHub Profile URL

Paste a URL in the form `https://github.com/{username}`. The parser calls the public GitHub REST API (`GET /users/{username}`) and extracts:

- `name` → `full_name`
- `email` → `emails` (only if the profile email is public)
- `bio` → `headline`
- `html_url` → `links.github`
- `blog` → `links.portfolio`
- `company` → `experience[].company`
- `location` → `location` (free text, parsed on commas)

No authentication token is required. The public API allows 60 requests per hour.

---

### Recruiter Notes (.txt)

A plain text file. The parser extracts the following by regex:

| What is extracted | How to format it |
|---|---|
| Candidate name | First line of the file, formatted `Firstname Lastname` |
| Email | Any email address found anywhere in the text |
| Phone | Any phone number in a common North American format |
| Skills | A line starting with `Skills:` followed by a comma-separated list |
| Education | A line in the format `Education: Institution, Degree, Year` |
| Years of experience | A phrase like `5 years of experience` anywhere in the text |

Example file:

```
Alice Smith
Spoke with Alice on June 20th. Strong Python and Java background.
Has 5 years of experience in distributed systems.
Skills: Python, Java, SQL, Kubernetes, Distributed Systems
Education: MIT, B.S. Computer Science, 2019
Email: alice@example.com
Phone: (408) 867-0001
```

See `samples/notes_alice.txt` and `samples/notes_bob.txt` for ready-to-use files.

---

## Output Config

Paste a JSON config into the textarea on the input page to control the shape of the output. If you leave it as the default, all fields are returned.

### Default config (full output)

```json
{
  "include_confidence": true,
  "include_provenance": true,
  "on_missing": "null"
}
```

### Custom config (select and rename fields)

```json
{
  "fields": [
    {"path": "full_name",      "type": "string",   "required": true},
    {"path": "primary_email",  "from": "emails[0]","type": "string", "required": true},
    {"path": "phone",          "from": "phones[0]","type": "string"},
    {"path": "skill_names",    "from": "skills[].name", "type": "string"}
  ],
  "include_confidence": true,
  "include_provenance": false,
  "on_missing": "null"
}
```

This config returns four fields only: the candidate's name (under the key `full_name`), their first email under the renamed key `primary_email`, their first phone, and a flat list of skill name strings under `skill_names`.

### Config field reference

**Top-level keys**

| Key | Type | Default | Description |
|---|---|---|---|
| `fields` | array | omit for full output | List of fields to include. Omit this key entirely to return the full canonical profile. |
| `include_confidence` | boolean | `true` | Include `overall_confidence` in the output and per-skill confidence scores. |
| `include_provenance` | boolean | `true` | Include the `provenance` array showing source and method for each field. |
| `on_missing` | string | `"null"` | What to do when a resolved field is null: `"null"` outputs null, `"omit"` skips the key, `"error"` throws if the field is also `required: true`. |

**Per-field keys (inside `fields` array)**

| Key | Type | Required | Description |
|---|---|---|---|
| `path` | string | yes | The output key name. |
| `from` | string | no | Source path expression. Defaults to `path` if omitted. |
| `type` | string | no | Type hint for documentation purposes (`"string"`, `"number"`, etc.). Not enforced at runtime. |
| `required` | boolean | no | If `true` and `on_missing` is `"error"`, throws when the value is null. |
| `normalize` | string | no | Apply a normalization pass to the resolved value: `"email"`, `"phone"`, `"date"`, `"country"`, or `"skill"`. |

**Supported `from` path expressions**

| Expression | Example | Resolves to |
|---|---|---|
| Direct field name | `"full_name"` | The field value directly |
| List index | `"emails[0]"` | First element of the list |
| Sub-field of every list element | `"skills[].name"` | Flat list of each skill's name |

---

## Sample Output

Running the four sample files together produces two candidates. Abbreviated output for Alice:

```json
{
  "candidate_id": "9f3c1a2b4e7d8f0a",
  "full_name": "Alice Smith",
  "emails": ["alice@example.com"],
  "phones": ["+14088670001"],
  "location": {
    "city": "San Francisco",
    "region": null,
    "country": "US"
  },
  "links": {
    "linkedin": "https://linkedin.com/in/alicesmith",
    "github": null,
    "portfolio": null
  },
  "headline": null,
  "years_experience": 5.0,
  "skills": [
    {"name": "Python",              "confidence": 0.70, "sources": ["recruiter_notes"]},
    {"name": "Java",                "confidence": 0.70, "sources": ["recruiter_notes"]},
    {"name": "SQL",                 "confidence": 0.75, "sources": ["recruiter_notes", "ats_json"]},
    {"name": "Kubernetes",          "confidence": 0.70, "sources": ["recruiter_notes"]},
    {"name": "Distributed Systems", "confidence": 0.70, "sources": ["recruiter_notes"]}
  ],
  "experience": [
    {"company": "Acme Corp", "title": "Senior Software Engineer", "start": null, "end": null, "summary": null}
  ],
  "education": [
    {"institution": "MIT", "degree": "B.S. Computer Science", "field": null, "end_year": 2019}
  ],
  "provenance": [
    {"field": "full_name",      "source": "ats_json",       "method": "direct"},
    {"field": "experience",     "source": "recruiter_csv",  "method": "direct"},
    {"field": "location",       "source": "ats_json",       "method": "direct"},
    {"field": "education",      "source": "ats_json",       "method": "direct"},
    {"field": "emails",         "source": "merged",         "method": "union"},
    {"field": "phones",         "source": "merged",         "method": "union"}
  ],
  "overall_confidence": 0.74
}
```

---

## Running Tests

```bash
mvn test
```

With a corporate proxy:

```bash
mvn test -s mvn-settings.xml
```

Expected result: **33 tests, 0 failures**.

The test suite covers: phone/email/date/country normalization (14 tests), CSV parser (4 tests), ATS JSON parser (5 tests), merge and conflict resolution (5 tests), and the projection layer (5 tests).

---

## Project Structure

```
candidate-transformer/
├── pom.xml                              Maven build file
├── mvn-settings.xml                     Maven Central settings (corporate proxy bypass)
├── configs/
│   ├── default.json                     Full passthrough config
│   └── custom.json                      Example field-select config
├── samples/
│   ├── candidates.csv                   3 sample candidates (recruiter CSV)
│   ├── ats_data.json                    2 candidates with ATS field names
│   ├── notes_alice.txt                  Recruiter notes for Alice
│   └── notes_bob.txt                    Recruiter notes for Bob
└── src/
    ├── main/java/com/eightfold/transformer/
    │   ├── CandidateTransformerApplication.java   Spring Boot entry point
    │   ├── controller/TransformController.java    HTTP endpoints (GET /, POST /transform)
    │   ├── model/                                 POJOs: CanonicalCandidate, RawCandidate, OutputConfig, ...
    │   ├── parser/                                One parser class per source type
    │   ├── pipeline/                              Normalizer, Merger, Scorer, Validator, Projector, PipelineRunner
    │   └── util/CandidateIdUtil.java              SHA-256[:16] candidate ID generation
    ├── main/resources/
    │   ├── application.yml                        Server config (port 8080, SNAKE_CASE JSON)
    │   └── templates/                             Thymeleaf templates (index.html, result.html)
    └── test/java/com/eightfold/transformer/
        ├── NormalizerTest.java
        ├── CsvParserTest.java
        ├── AtsJsonParserTest.java
        ├── MergerTest.java
        └── ProjectorTest.java
```

---

## Notes

- **Phone numbers** — the normalizer uses Google's libphonenumber and defaults to the US country code for ambiguous numbers. Numbers that cannot be resolved to E.164 are stored as `null` rather than the raw string.
- **Identity matching** — candidates are matched across sources by their normalized primary email. Name-based fuzzy matching is deliberately excluded to avoid false positives.
- **GitHub rate limit** — the public GitHub API allows 60 unauthenticated requests per hour, which is sufficient for demo usage.
- **LinkedIn** — not implemented; requires OAuth and is noted as out of scope.
