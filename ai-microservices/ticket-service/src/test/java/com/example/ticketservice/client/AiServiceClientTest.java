package com.example.ticketservice.client;

import com.example.ticketservice.model.AiInsights;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class AiServiceClientTest {

    private static final String LOOPBACK = "127.0.0.1";

    private static final String ANALYSIS_JSON = """
            {
              "category": "BILLING",
              "priority": "HIGH",
              "sentiment": "NEGATIVE",
              "summary": "Customer was charged twice.",
              "suggestedReply": "Sorry about that, we have refunded the duplicate charge.",
              "engine": "claude",
              "fallbackReason": "ignored by ticket-service",
              "confidence": 0.93,
              "tags": ["refund"],
              "meta": {"latencyMs": 812}
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<String> receivedMethod = new AtomicReference<>();
    private final AtomicReference<String> receivedContentType = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();

    private HttpServer server;
    private ExecutorService serverThreads;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress(LOOPBACK, 0), 0);
        // Handlers run off the dispatcher thread so a deliberately slow one cannot block shutdown.
        serverThreads = Executors.newCachedThreadPool();
        server.setExecutor(serverThreads);
        server.start();
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
        serverThreads.shutdownNow();
    }

    @Test
    void mapsAllFieldsAndIgnoresUnknownOnes() {
        stubAnalyze(200, ANALYSIS_JSON, Duration.ZERO);

        Optional<AiInsights> insights = client(Duration.ofSeconds(5)).analyze("Double charge", "I was billed twice.");

        assertThat(insights).contains(new AiInsights(
                "BILLING",
                "HIGH",
                "NEGATIVE",
                "Customer was charged twice.",
                "Sorry about that, we have refunded the duplicate charge.",
                "claude"));
    }

    @Test
    void postsSubjectAndBodyAsJson() throws IOException {
        stubAnalyze(200, ANALYSIS_JSON, Duration.ZERO);

        client(Duration.ofSeconds(5)).analyze("Double charge", "I was billed twice.");

        assertThat(receivedMethod.get()).isEqualTo("POST");
        assertThat(receivedContentType.get()).startsWith("application/json");
        JsonNode sent = objectMapper.readTree(receivedBody.get());
        assertThat(sent.path("subject").asText()).isEqualTo("Double charge");
        assertThat(sent.path("body").asText()).isEqualTo("I was billed twice.");
    }

    @Test
    void passesThroughEnumValuesItDoesNotKnow() {
        stubAnalyze(200, """
                {"category": "LEGAL", "priority": "CRITICAL", "sentiment": "MIXED",
                 "summary": "s", "suggestedReply": "r", "engine": "rules"}
                """, Duration.ZERO);

        Optional<AiInsights> insights = client(Duration.ofSeconds(5)).analyze("subject", "body");

        assertThat(insights).hasValueSatisfying(ai -> {
            assertThat(ai.category()).isEqualTo("LEGAL");
            assertThat(ai.priority()).isEqualTo("CRITICAL");
            assertThat(ai.sentiment()).isEqualTo("MIXED");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 500, 503})
    void errorStatusYieldsEmpty(int status) {
        stubAnalyze(status, "{\"error\": \"nope\"}", Duration.ZERO);

        assertThat(client(Duration.ofSeconds(5)).analyze("subject", "body")).isEmpty();
    }

    @Test
    void malformedJsonYieldsEmpty() {
        stubAnalyze(200, "this is not json", Duration.ZERO);

        assertThat(client(Duration.ofSeconds(5)).analyze("subject", "body")).isEmpty();
    }

    @Test
    void unreachableServiceYieldsEmpty() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK))) {
            closedPort = socket.getLocalPort();
        }
        AiServiceClient client = new AiServiceClient(RestClient.builder(),
                "http://" + LOOPBACK + ":" + closedPort, Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThat(client.analyze("subject", "body")).isEmpty();
    }

    @Test
    void responseSlowerThanReadTimeoutYieldsEmpty() {
        stubAnalyze(200, ANALYSIS_JSON, Duration.ofSeconds(5));

        long start = System.nanoTime();
        Optional<AiInsights> insights = client(Duration.ofMillis(300)).analyze("subject", "body");
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(insights).isEmpty();
        assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
    }

    private AiServiceClient client(Duration readTimeout) {
        String baseUrl = "http://" + LOOPBACK + ":" + server.getAddress().getPort();
        return new AiServiceClient(RestClient.builder(), baseUrl, Duration.ofSeconds(1), readTimeout);
    }

    private void stubAnalyze(int status, String responseBody, Duration delay) {
        server.createContext("/api/ai/analyze", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exchange.close();
                return;
            }
            byte[] bytes = responseBody.getBytes(UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }
}
