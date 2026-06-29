package com.eightfold.transformer.pipeline;

import com.eightfold.transformer.model.CanonicalCandidate;
import com.eightfold.transformer.model.OutputConfig;
import com.eightfold.transformer.model.RawCandidate;
import com.eightfold.transformer.parser.AtsJsonParser;
import com.eightfold.transformer.parser.CsvParser;
import com.eightfold.transformer.parser.GitHubParser;
import com.eightfold.transformer.parser.NotesParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PipelineRunner {

    private static final Logger log = LoggerFactory.getLogger(PipelineRunner.class);

    private final CsvParser csvParser;
    private final AtsJsonParser atsJsonParser;
    private final GitHubParser gitHubParser;
    private final NotesParser notesParser;
    private final Merger merger;
    private final Scorer scorer;
    private final Validator validator;
    private final Projector projector;

    public PipelineRunner(CsvParser csvParser, AtsJsonParser atsJsonParser,
                          GitHubParser gitHubParser, NotesParser notesParser,
                          Merger merger, Scorer scorer, Validator validator, Projector projector) {
        this.csvParser = csvParser;
        this.atsJsonParser = atsJsonParser;
        this.gitHubParser = gitHubParser;
        this.notesParser = notesParser;
        this.merger = merger;
        this.scorer = scorer;
        this.validator = validator;
        this.projector = projector;
    }

    public List<Map<String, Object>> run(MultipartFile csvFile,
                                          MultipartFile atsJsonFile,
                                          MultipartFile notesFile,
                                          String githubUrl,
                                          OutputConfig config) {
        List<RawCandidate> allRaw = new ArrayList<>();

        // 1. Parse each source — failures are logged and skipped
        allRaw.addAll(parseFile(csvFile, "CSV", () -> csvParser.parse(csvFile.getInputStream())));
        allRaw.addAll(parseFile(atsJsonFile, "ATS JSON", () -> atsJsonParser.parse(atsJsonFile.getInputStream())));
        allRaw.addAll(parseFile(notesFile, "Notes", () -> {
            RawCandidate c = notesParser.parse(notesFile.getInputStream(), notesFile.getOriginalFilename());
            return c != null ? List.of(c) : List.of();
        }));

        if (githubUrl != null && !githubUrl.isBlank()) {
            try {
                RawCandidate ghCandidate = gitHubParser.parse(githubUrl);
                if (ghCandidate != null) allRaw.add(ghCandidate);
            } catch (Exception e) {
                log.warn("GitHub parser failed: {}", e.getMessage());
            }
        }

        // 2. Merge into canonical candidates
        List<CanonicalCandidate> canonical = merger.merge(allRaw);

        // 3. Score each candidate
        canonical.forEach(scorer::score);

        // 4. Validate each candidate before projecting
        canonical.forEach(validator::validate);

        // 5. Project using config and return
        return canonical.stream()
                .map(c -> projector.project(c, config))
                .toList();
    }

    private List<RawCandidate> parseFile(MultipartFile file, String label,
                                          ParseAction action) {
        if (file == null || file.isEmpty()) return List.of();
        try {
            return action.run();
        } catch (IOException e) {
            log.warn("{} parser failed: {}", label, e.getMessage());
            return List.of();
        }
    }

    @FunctionalInterface
    interface ParseAction {
        List<RawCandidate> run() throws IOException;
    }
}
