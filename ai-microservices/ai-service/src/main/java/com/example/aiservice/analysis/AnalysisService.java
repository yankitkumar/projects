package com.example.aiservice.analysis;

import com.example.aiservice.api.AnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AnalysisService {

    public static final String ENGINE_CLAUDE = "claude";
    public static final String ENGINE_RULES = "rules";

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final Optional<ClaudeAnalyzer> claude;
    private final RuleBasedAnalyzer rules;

    // Claude is optional: the bean only exists when an API key is configured.
    public AnalysisService(Optional<ClaudeAnalyzer> claude, RuleBasedAnalyzer rules) {
        this.claude = claude;
        this.rules = rules;
    }

    public AnalyzeResponse analyze(String subject, String body) {
        if (claude.isEmpty()) {
            return AnalyzeResponse.of(rules.analyze(subject, body), ENGINE_RULES, null);
        }
        try {
            return AnalyzeResponse.of(claude.get().analyze(subject, body), ENGINE_CLAUDE, null);
        } catch (AnalysisException e) {
            log.warn("Claude analysis failed, falling back to rules: {}", e.getMessage());
            return AnalyzeResponse.of(rules.analyze(subject, body), ENGINE_RULES, e.getMessage());
        }
    }

    public String activeEngine() {
        return claude.isPresent() ? ENGINE_CLAUDE : ENGINE_RULES;
    }

    public String model() {
        return claude.map(ClaudeAnalyzer::model).orElse(null);
    }
}
