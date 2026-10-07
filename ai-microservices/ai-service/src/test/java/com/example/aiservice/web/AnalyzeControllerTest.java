package com.example.aiservice.web;

import com.example.aiservice.analysis.ClaudeAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The blank key overrides any ANTHROPIC_API_KEY in the caller's environment, so these tests never call Claude.
@SpringBootTest(properties = "ai.anthropic.api-key=")
@AutoConfigureMockMvc
class AnalyzeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void noClaudeBeanWithoutApiKey() {
        assertThat(context.getBeanNamesForType(ClaudeAnalyzer.class)).isEmpty();
    }

    @Test
    void analyzesWithRules() throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject": "Can't log in", "body": "I am locked out of my account. Please help."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ACCOUNT"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.sentiment").value("NEUTRAL"))
                .andExpect(jsonPath("$.summary").value("I am locked out of my account."))
                .andExpect(jsonPath("$.suggestedReply").isNotEmpty())
                .andExpect(jsonPath("$.engine").value("rules"))
                .andExpect(jsonPath("$.fallbackReason").doesNotExist());
    }

    @Test
    void rejectsBlankAndOversizedFields() throws Exception {
        String longBody = "x".repeat(5001);
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\": \" \", \"body\": \"" + longBody + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subject").value("subject must not be blank"))
                .andExpect(jsonPath("$.body").value("body must be at most 5000 characters"));
    }

    @Test
    void rejectsMissingFields() throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subject").value("subject must not be blank"))
                .andExpect(jsonPath("$.body").value("body must not be blank"));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/ai/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\": \"Hello\", "))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\": \"Malformed JSON request body\"}", true));
    }

    @Test
    void infoReportsRulesEngineAndNoModel() throws Exception {
        mockMvc.perform(get("/api/ai/info"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"engine\": \"rules\", \"model\": null}", true));
    }
}
