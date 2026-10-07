package com.example.aiservice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * A stand-in for the Claude Messages API on 127.0.0.1 that records the last request and replies
 * with a canned response, so tests exercise the real SDK without touching the network.
 */
public class StubAnthropicServer implements AutoCloseable {

    public record Request(String method, String path, Map<String, String> headers, String body) {

        public String header(String name) {
            return headers.get(name);
        }
    }

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String responseBody = "{}";
    private volatile Request lastRequest;

    public StubAnthropicServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void respond(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    public Request lastRequest() {
        return lastRequest;
    }

    private void handle(HttpExchange exchange) throws IOException {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, String.join(",", values)));
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        lastRequest = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), headers, body);

        byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /** A Messages API response; {@code content} and {@code stopDetails} are raw JSON. */
    public static String message(String content, String stopReason, String stopDetails) {
        return """
                {"id": "msg_test", "type": "message", "role": "assistant", "model": "claude-opus-5-5",
                 "content": %s, "stop_reason": "%s", "stop_sequence": null, "stop_details": %s,
                 "usage": {"input_tokens": 120, "output_tokens": 80}}
                """.formatted(content, stopReason, stopDetails);
    }

    /** A successful structured-output response: an (empty) thinking block followed by the JSON text. */
    public static String analysisMessage() {
        String analysis = """
                {"category": "BILLING", "priority": "HIGH", "sentiment": "NEGATIVE",
                 "summary": "Customer was charged twice for one subscription.",
                 "suggestedReply": "Thanks for letting us know. We are reviewing the charges on your account."}""";
        String escaped = analysis.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String content = """
                [{"type": "thinking", "thinking": "", "signature": "sig"},
                 {"type": "text", "text": "%s"}]""".formatted(escaped);
        return message(content, "end_turn", "null");
    }

    public static String refusalMessage() {
        return message("[]", "refusal", """
                {"type": "refusal", "category": "cyber", "explanation": null}""");
    }

    public static String error(String type, String message) {
        return """
                {"type": "error", "error": {"type": "%s", "message": "%s"}}""".formatted(type, message);
    }
}
