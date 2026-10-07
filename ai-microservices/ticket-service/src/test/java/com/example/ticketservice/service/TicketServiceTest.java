package com.example.ticketservice.service;

import com.example.ticketservice.api.CreateTicketRequest;
import com.example.ticketservice.api.TicketStats;
import com.example.ticketservice.client.AiServiceClient;
import com.example.ticketservice.model.AiInsights;
import com.example.ticketservice.model.AiStatus;
import com.example.ticketservice.model.Ticket;
import com.example.ticketservice.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private final AiServiceClient aiServiceClient = mock(AiServiceClient.class);
    private final TicketService ticketService = new TicketService(aiServiceClient);

    @Test
    void createStoresOpenTicketWithInsights() {
        AiInsights insights = insights("BILLING", "HIGH");
        when(aiServiceClient.analyze("Double charge", "I was billed twice."))
                .thenReturn(Optional.of(insights));

        Ticket ticket = ticketService.create(
                new CreateTicketRequest("Double charge", "I was billed twice.", "jane@example.com"));

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getCreatedAt()).isNotNull();
        assertThat(ticket.getCustomerEmail()).isEqualTo("jane@example.com");
        assertThat(ticket.getAi()).isEqualTo(insights);
        assertThat(ticket.getAiStatus()).isEqualTo(AiStatus.ANALYZED);
        assertThat(ticketService.findById(ticket.getId())).contains(ticket);
    }

    @Test
    void createStillStoresTicketWhenAiIsUnavailable() {
        Ticket ticket = create("Printer on fire", null);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(ticket.getAi()).isNull();
        assertThat(ticket.getAiStatus()).isEqualTo(AiStatus.UNAVAILABLE);
        assertThat(ticketService.findById(ticket.getId())).contains(ticket);
    }

    @Test
    void findAllSortsByPriorityThenIdWithUnanalyzedAndUnknownLast() {
        Ticket low = create("low", insights("OTHER", "LOW"));
        Ticket unanalyzed = create("unanalyzed", null);
        Ticket urgent = create("urgent", insights("OTHER", "URGENT"));
        Ticket unknown = create("unknown", insights("OTHER", "SOMEDAY"));
        Ticket high = create("high", insights("OTHER", "HIGH"));
        Ticket medium = create("medium", insights("OTHER", "MEDIUM"));
        Ticket lowerCaseUrgent = create("urgent too", insights("OTHER", "urgent"));

        assertThat(ticketService.findAll(null, null, null))
                .containsExactly(urgent, lowerCaseUrgent, high, medium, low, unanalyzed, unknown);
    }

    @Test
    void findAllFiltersCaseInsensitivelyAndIgnoresBlankFilters() {
        Ticket billingHigh = create("billing high", insights("BILLING", "HIGH"));
        Ticket technicalHigh = create("technical high", insights("TECHNICAL", "HIGH"));
        Ticket billingLow = create("billing low", insights("BILLING", "LOW"));
        Ticket unanalyzed = create("unanalyzed", null);
        ticketService.resolve(billingLow.getId());

        assertThat(ticketService.findAll(null, "high", null)).containsExactly(billingHigh, technicalHigh);
        assertThat(ticketService.findAll(null, null, "Billing")).containsExactly(billingHigh, billingLow);
        assertThat(ticketService.findAll("resolved", null, null)).containsExactly(billingLow);
        assertThat(ticketService.findAll("OPEN", null, "billing")).containsExactly(billingHigh);
        assertThat(ticketService.findAll("", " ", null))
                .containsExactly(billingHigh, technicalHigh, billingLow, unanalyzed);
        assertThat(ticketService.findAll(null, "urgent", null)).isEmpty();
    }

    @Test
    void statsCountsStatusesCategoriesAndPriorities() {
        create("a", insights("BILLING", "HIGH"));
        create("b", insights("BILLING", "LOW"));
        Ticket technical = create("c", insights("TECHNICAL", "HIGH"));
        create("d", null);
        ticketService.resolve(technical.getId());

        TicketStats stats = ticketService.stats();

        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.open()).isEqualTo(3);
        assertThat(stats.resolved()).isEqualTo(1);
        assertThat(stats.unanalyzed()).isEqualTo(1);
        assertThat(stats.byCategory()).isEqualTo(Map.of("BILLING", 2L, "TECHNICAL", 1L));
        assertThat(stats.byPriority()).isEqualTo(Map.of("HIGH", 2L, "LOW", 1L));
    }

    @Test
    void resolveMarksTicketResolved() {
        Ticket ticket = create("Where is my parcel?", insights("SHIPPING", "MEDIUM"));

        assertThat(ticketService.resolve(ticket.getId()))
                .hasValueSatisfying(t -> assertThat(t.getStatus()).isEqualTo(TicketStatus.RESOLVED));
        assertThat(ticketService.resolve(999L)).isEmpty();
    }

    @Test
    void reanalyzeStoresFreshInsights() {
        Ticket ticket = create("Login loop", null);
        AiInsights insights = insights("ACCOUNT", "URGENT");
        when(aiServiceClient.analyze(eq("Login loop"), anyString())).thenReturn(Optional.of(insights));

        Optional<Ticket> reanalyzed = ticketService.reanalyze(ticket.getId());

        assertThat(reanalyzed).hasValueSatisfying(t -> {
            assertThat(t.getAi()).isEqualTo(insights);
            assertThat(t.getAiStatus()).isEqualTo(AiStatus.ANALYZED);
        });
    }

    @Test
    void reanalyzeKeepsPreviousInsightsWhenAiFails() {
        AiInsights original = insights("ACCOUNT", "HIGH");
        Ticket ticket = create("Password reset", original);
        when(aiServiceClient.analyze(eq("Password reset"), anyString())).thenReturn(Optional.empty());

        Optional<Ticket> reanalyzed = ticketService.reanalyze(ticket.getId());

        assertThat(reanalyzed).hasValueSatisfying(t -> {
            assertThat(t.getAi()).isEqualTo(original);
            assertThat(t.getAiStatus()).isEqualTo(AiStatus.ANALYZED);
        });
    }

    @Test
    void reanalyzeOfUnknownTicketDoesNotCallAi() {
        assertThat(ticketService.reanalyze(42L)).isEmpty();
        verifyNoInteractions(aiServiceClient);
    }

    @Test
    void deleteRemovesTicket() {
        Ticket ticket = create("Great service", insights("FEEDBACK", "LOW"));

        assertThat(ticketService.delete(ticket.getId())).isTrue();
        assertThat(ticketService.findById(ticket.getId())).isEmpty();
        assertThat(ticketService.delete(ticket.getId())).isFalse();
        assertThat(ticketService.findAll(null, null, null)).isEmpty();
    }

    private Ticket create(String subject, AiInsights insights) {
        when(aiServiceClient.analyze(eq(subject), anyString())).thenReturn(Optional.ofNullable(insights));
        return ticketService.create(new CreateTicketRequest(subject, "Details about " + subject, null));
    }

    private static AiInsights insights(String category, String priority) {
        return new AiInsights(category, priority, "NEUTRAL", "summary", "suggested reply", "rules");
    }
}
