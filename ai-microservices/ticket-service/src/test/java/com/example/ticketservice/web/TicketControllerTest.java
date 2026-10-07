package com.example.ticketservice.web;

import com.example.ticketservice.api.CreateTicketRequest;
import com.example.ticketservice.client.AiServiceClient;
import com.example.ticketservice.model.AiInsights;
import com.example.ticketservice.service.TicketService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketService ticketService;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    // The service is an in-memory singleton shared by every test in this context.
    @BeforeEach
    void removeAllTickets() {
        ticketService.findAll(null, null, null).forEach(t -> ticketService.delete(t.getId()));
    }

    @Test
    void createReturns201WithLocationAndInsights() throws Exception {
        when(aiServiceClient.analyze("Double charge", "I was billed twice."))
                .thenReturn(Optional.of(insights("BILLING", "HIGH")));

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"subject": "Double charge", "body": "I was billed twice.",
                                 "customerEmail": "jane@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.customerEmail").value("jane@example.com"))
                .andExpect(jsonPath("$.aiStatus").value("ANALYZED"))
                .andExpect(jsonPath("$.ai.category").value("BILLING"))
                .andExpect(jsonPath("$.ai.priority").value("HIGH"))
                .andExpect(jsonPath("$.ai.engine").value("rules"))
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String location = result.getResponse().getHeader("Location");
        assertThat(location).isEqualTo("/api/tickets/" + id);
        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Double charge"));
    }

    @Test
    void createSucceedsWhenAiServiceIsDown() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"subject\": \"App crashes\", \"body\": \"On startup.\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.aiStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.ai").value(nullValue()));
    }

    @Test
    void createRejectsBlankSubject() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"subject\": \"  \", \"body\": \"Something broke.\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subject").value("subject must not be blank"));

        verifyNoInteractions(aiServiceClient);
    }

    @Test
    void createRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"subject": "Hi", "body": "Something broke.", "customerEmail": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.customerEmail").value("customerEmail must be a valid email address"));
    }

    @Test
    void createRejectsOverlongSubject() throws Exception {
        String subject = "x".repeat(201);
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"subject\": \"" + subject + "\", \"body\": \"b\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subject").value("subject must be at most 200 characters"));
    }

    @Test
    void createRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(APPLICATION_JSON)
                        .content("{\"subject\": \"Hi\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request body"));
    }

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/tickets/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
        mockMvc.perform(post("/api/tickets/abc/resolve"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isString());
    }

    @Test
    void unknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/tickets/999999")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/tickets/999999/analyze")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/tickets/999999/resolve")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/tickets/999999")).andExpect(status().isNotFound());
    }

    @Test
    void listIsSortedByPriorityAndFilterable() throws Exception {
        createTicket("Refund please", insights("BILLING", "LOW"));
        createTicket("Site is down", insights("TECHNICAL", "URGENT"));
        createTicket("Charged twice", insights("BILLING", "HIGH"));
        createTicket("No analysis", null);

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].subject").value("Site is down"))
                .andExpect(jsonPath("$[3].subject").value("No analysis"));

        mockMvc.perform(get("/api/tickets").param("category", "billing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].subject").value("Charged twice"))
                .andExpect(jsonPath("$[1].subject").value("Refund please"));
    }

    @Test
    void statsRouteIsNotMistakenForAnId() throws Exception {
        createTicket("Charged twice", insights("BILLING", "HIGH"));
        long resolvedId = createTicket("Parcel late", insights("SHIPPING", "MEDIUM"));
        createTicket("No analysis", null);
        ticketService.resolve(resolvedId);

        mockMvc.perform(get("/api/tickets/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.open").value(2))
                .andExpect(jsonPath("$.resolved").value(1))
                .andExpect(jsonPath("$.unanalyzed").value(1))
                .andExpect(jsonPath("$.byCategory.BILLING").value(1))
                .andExpect(jsonPath("$.byPriority.MEDIUM").value(1));
    }

    @Test
    void analyzeReRunsAiForExistingTicket() throws Exception {
        long id = createTicket("Cannot log in", null);
        when(aiServiceClient.analyze(eq("Cannot log in"), anyString()))
                .thenReturn(Optional.of(insights("ACCOUNT", "URGENT")));

        mockMvc.perform(post("/api/tickets/{id}/analyze", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiStatus").value("ANALYZED"))
                .andExpect(jsonPath("$.ai.category").value("ACCOUNT"));
    }

    @Test
    void resolveMarksTicketResolved() throws Exception {
        long id = createTicket("Thanks!", insights("FEEDBACK", "LOW"));

        mockMvc.perform(post("/api/tickets/{id}/resolve", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void deleteReturns204AndTicketIsGone() throws Exception {
        long id = createTicket("Duplicate ticket", null);

        mockMvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tickets/{id}", id)).andExpect(status().isNotFound());
    }

    private long createTicket(String subject, AiInsights insights) {
        when(aiServiceClient.analyze(eq(subject), anyString())).thenReturn(Optional.ofNullable(insights));
        return ticketService.create(new CreateTicketRequest(subject, "Details about " + subject, null)).getId();
    }

    private static AiInsights insights(String category, String priority) {
        return new AiInsights(category, priority, "NEUTRAL", "summary", "suggested reply", "rules");
    }
}
