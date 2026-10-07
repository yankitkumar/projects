package com.example.aiservice.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.example.aiservice.analysis.ClaudeAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Wires Claude in only when an API key is set. {@code @ConditionalOnProperty} is not enough here:
 * it treats {@code ai.anthropic.api-key=} (empty) as present.
 */
@Configuration
@ConditionalOnExpression("!'${ai.anthropic.api-key:}'.isBlank()")
public class AnthropicConfig {

    // Built explicitly rather than with fromEnv() so the SDK never picks up ANTHROPIC_* variables
    // behind Spring's back; everything comes from ai.anthropic.* properties.
    @Bean
    public AnthropicClient anthropicClient(@Value("${ai.anthropic.api-key}") String apiKey,
                                           @Value("${ai.anthropic.base-url:}") String baseUrl) {
        // Timeouts are retried, so the worst case is about 40 s x 2 attempts. That keeps the rules
        // fallback inside ticket-service's 90 s read timeout instead of arriving after it gave up.
        AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(40))
                .maxRetries(1);
        if (!baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    @Bean
    public ClaudeAnalyzer claudeAnalyzer(AnthropicClient anthropicClient,
                                         @Value("${ai.anthropic.model:claude-opus-5-5}") String model) {
        return new ClaudeAnalyzer(anthropicClient, model);
    }
}
