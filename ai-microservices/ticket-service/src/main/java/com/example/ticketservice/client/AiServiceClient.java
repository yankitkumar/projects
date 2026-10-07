package com.example.ticketservice.client;

import com.example.ticketservice.model.AiInsights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;

    public AiServiceClient(RestClient.Builder builder,
                           @Value("${ai-service.url}") String baseUrl,
                           @Value("${ai-service.connect-timeout}") Duration connectTimeout,
                           @Value("${ai-service.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                // ai-service is plain HTTP/1.1; this skips the client's h2c upgrade attempt.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    // AI analysis is an enrichment, not a requirement: any failure (ai-service down, slow,
    // erroring or returning junk) is logged and reported as "no insights" so tickets can
    // still be created.
    public Optional<AiInsights> analyze(String subject, String body) {
        try {
            AiInsights insights = restClient.post()
                    .uri("/api/ai/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new AnalyzeRequest(subject, body))
                    .retrieve()
                    .body(AiInsights.class);
            return Optional.ofNullable(insights);
        } catch (RuntimeException ex) {
            // Log the direct cause (e.g. ConnectException): when ai-service is not running the
            // wrapper's own message just ends in "null".
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.warn("ai-service analysis failed, continuing without insights: {}", cause.toString());
            return Optional.empty();
        }
    }

    record AnalyzeRequest(String subject, String body) {
    }
}
