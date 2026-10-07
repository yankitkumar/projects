package com.example.ticketservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Full wiring with the real AiServiceClient (no mocks), pointed at a port nothing listens on.
@SpringBootTest
@AutoConfigureMockMvc
class TicketServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void pointAiServiceAtClosedPort(DynamicPropertyRegistry registry) throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            closedPort = socket.getLocalPort();
        }
        registry.add("ai-service.url", () -> "http://127.0.0.1:" + closedPort);
    }

    @Test
    void ticketIsCreatedUnanalyzedWhenAiServiceIsUnreachable() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"subject\": \"Order missing\", \"body\": \"Nothing arrived.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.aiStatus").value("UNAVAILABLE"));
    }
}
