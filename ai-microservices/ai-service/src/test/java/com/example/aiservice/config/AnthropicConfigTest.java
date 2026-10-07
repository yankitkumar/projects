package com.example.aiservice.config;

import com.example.aiservice.StubAnthropicServer;
import com.example.aiservice.analysis.ClaudeAnalyzer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With an API key set, the Claude path is wired in; requests go to a local stub, never to the real API. */
@SpringBootTest
@AutoConfigureMockMvc
class AnthropicConfigTest {

    private static final StubAnthropicServer STUB = startStub();

    @DynamicPropertySource
    static void anthropicProperties(DynamicPropertyRegistry registry) {
        registry.add("ai.anthropic.api-key", () -> "test-key");
        registry.add("ai.anthropic.model", () -> "claude-opus-5-5");
        registry.add("ai.anthropic.base-url", STUB::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClaudeAnalyzer claudeAnalyzer;

    @AfterAll
    static void stopStub() {
        STUB.close();
    }

    @Test
    void createsClaudeAnalyzerWhenApiKeyIsSet() throws Exception {
        assertThat(claudeAnalyzer.model()).isEqualTo("claude-opus-5-5");

        mockMvc.perform(get("/api/ai/info"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"engine\": \"claude\", \"model\": \"claude-opus-5-5\"}", true));
    }

    @Test
    void analyzesThroughClaude() throws Exception {
        STUB.respond(200, StubAnthropicServer.analysisMessage());

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\": \"Charged twice\", \"body\": \"I was charged twice this month.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("claude"))
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andExpect(jsonPath("$.summary").value("Customer was charged twice for one subscription."))
                .andExpect(jsonPath("$.fallbackReason").doesNotExist());
        assertThat(STUB.lastRequest().header("x-api-key")).isEqualTo("test-key");
    }

    @Test
    void fallsBackToRulesWhenClaudeFails() throws Exception {
        // 401 is not retried, so the test does not wait on the client's retry backoff.
        STUB.respond(401, StubAnthropicServer.error("authentication_error", "invalid x-api-key"));

        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\": \"Charged twice\", \"body\": \"I was charged twice this month.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("rules"))
                .andExpect(jsonPath("$.category").value("BILLING"))
                .andExpect(jsonPath("$.fallbackReason").value("Claude API returned HTTP 401"));
    }

    private static StubAnthropicServer startStub() {
        try {
            return new StubAnthropicServer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
