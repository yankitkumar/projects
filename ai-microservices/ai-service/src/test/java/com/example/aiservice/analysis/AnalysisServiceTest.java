package com.example.aiservice.analysis;

import com.example.aiservice.api.AnalyzeResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    private static final String SUBJECT = "Charged twice";
    private static final String BODY = "I was charged twice for my subscription. This is unacceptable.";

    private final ClaudeAnalyzer claude = mock(ClaudeAnalyzer.class);
    private final RuleBasedAnalyzer rules = new RuleBasedAnalyzer();

    @Test
    void usesClaudeWhenItSucceeds() {
        TicketAnalysis fromClaude = new TicketAnalysis(Category.BILLING, Priority.HIGH, Sentiment.NEGATIVE,
                "Customer was charged twice.", "Thanks for reaching out.");
        when(claude.analyze(SUBJECT, BODY)).thenReturn(fromClaude);
        AnalysisService service = new AnalysisService(Optional.of(claude), rules);

        AnalyzeResponse response = service.analyze(SUBJECT, BODY);

        assertThat(response.engine()).isEqualTo("claude");
        assertThat(response.summary()).isEqualTo("Customer was charged twice.");
        assertThat(response.fallbackReason()).isNull();
    }

    @Test
    void fallsBackToRulesWithReasonWhenClaudeFails() {
        when(claude.analyze(SUBJECT, BODY)).thenThrow(new AnalysisException("declined by safety classifier (cyber)"));
        AnalysisService service = new AnalysisService(Optional.of(claude), rules);

        AnalyzeResponse response = service.analyze(SUBJECT, BODY);

        assertThat(response.engine()).isEqualTo("rules");
        assertThat(response.fallbackReason()).isEqualTo("declined by safety classifier (cyber)");
        assertThat(response.category()).isEqualTo(Category.BILLING);
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
        assertThat(response.sentiment()).isEqualTo(Sentiment.NEGATIVE);
    }

    @Test
    void usesRulesWithoutReasonWhenClaudeIsNotConfigured() {
        AnalysisService service = new AnalysisService(Optional.empty(), rules);

        AnalyzeResponse response = service.analyze(SUBJECT, BODY);

        assertThat(response.engine()).isEqualTo("rules");
        assertThat(response.fallbackReason()).isNull();
        assertThat(response.category()).isEqualTo(Category.BILLING);
    }

    @Test
    void reportsActiveEngineAndModel() {
        when(claude.model()).thenReturn("claude-opus-5-5");

        AnalysisService withClaude = new AnalysisService(Optional.of(claude), rules);
        AnalysisService rulesOnly = new AnalysisService(Optional.empty(), rules);

        assertThat(withClaude.activeEngine()).isEqualTo("claude");
        assertThat(withClaude.model()).isEqualTo("claude-opus-5-5");
        assertThat(rulesOnly.activeEngine()).isEqualTo("rules");
        assertThat(rulesOnly.model()).isNull();
    }
}
