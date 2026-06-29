package com.eightfold.transformer.controller;

import com.eightfold.transformer.model.OutputConfig;
import com.eightfold.transformer.pipeline.PipelineRunner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
public class TransformController {

    private static final Logger log = LoggerFactory.getLogger(TransformController.class);

    private final PipelineRunner pipelineRunner;
    private final ObjectMapper objectMapper;

    public TransformController(PipelineRunner pipelineRunner, ObjectMapper objectMapper) {
        this.pipelineRunner = pipelineRunner;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("defaultConfig",
                "{\n  \"include_confidence\": true,\n  \"include_provenance\": true,\n  \"on_missing\": \"null\"\n}");
        return "index";
    }

    @PostMapping("/transform")
    public String transform(
            @RequestParam(value = "csvFile", required = false) MultipartFile csvFile,
            @RequestParam(value = "atsJsonFile", required = false) MultipartFile atsJsonFile,
            @RequestParam(value = "notesFile", required = false) MultipartFile notesFile,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "configJson", required = false) String configJson,
            Model model) {

        // Parse the output config (or use defaults if blank)
        OutputConfig config = parseConfig(configJson);

        // Run the pipeline
        List<Map<String, Object>> results = pipelineRunner.run(
                csvFile, atsJsonFile, notesFile, githubUrl, config);

        // Pretty-print each result for display
        List<String> prettyResults = results.stream()
                .map(r -> {
                    try {
                        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(r);
                    } catch (JsonProcessingException e) {
                        return "{}";
                    }
                })
                .toList();

        // Full JSON array for download
        String fullJson;
        try {
            fullJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
        } catch (JsonProcessingException e) {
            fullJson = "[]";
        }

        model.addAttribute("candidates", prettyResults);
        model.addAttribute("fullJson", fullJson);
        model.addAttribute("count", results.size());

        return "result";
    }

    private OutputConfig parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return new OutputConfig();
        }
        try {
            return objectMapper.readValue(configJson, OutputConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("Invalid config JSON, using defaults: {}", e.getMessage());
            return new OutputConfig();
        }
    }
}
