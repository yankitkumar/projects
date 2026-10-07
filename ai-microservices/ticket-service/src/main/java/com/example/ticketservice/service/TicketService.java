package com.example.ticketservice.service;

import com.example.ticketservice.api.CreateTicketRequest;
import com.example.ticketservice.api.TicketStats;
import com.example.ticketservice.client.AiServiceClient;
import com.example.ticketservice.model.AiInsights;
import com.example.ticketservice.model.AiStatus;
import com.example.ticketservice.model.Ticket;
import com.example.ticketservice.model.TicketStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final List<String> PRIORITY_ORDER = List.of("URGENT", "HIGH", "MEDIUM", "LOW");

    private static final Comparator<Ticket> BY_PRIORITY_THEN_ID =
            Comparator.comparingInt(TicketService::priorityRank).thenComparing(Ticket::getId);

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);
    private final AiServiceClient aiServiceClient;

    public TicketService(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    public Ticket create(CreateTicketRequest request) {
        Ticket ticket = new Ticket(idSequence.incrementAndGet(), request.subject(), request.body(),
                request.customerEmail(), Instant.now());
        Optional<AiInsights> insights = aiServiceClient.analyze(ticket.getSubject(), ticket.getBody());
        ticket.setAi(insights.orElse(null));
        ticket.setAiStatus(insights.isPresent() ? AiStatus.ANALYZED : AiStatus.UNAVAILABLE);
        // Stored only after analysis so readers never see a ticket without an aiStatus.
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    public List<Ticket> findAll(String status, String priority, String category) {
        return tickets.values().stream()
                .filter(t -> matches(status, t.getStatus().name()))
                .filter(t -> matches(priority, t.getAi() == null ? null : t.getAi().priority()))
                .filter(t -> matches(category, t.getAi() == null ? null : t.getAi().category()))
                .sorted(BY_PRIORITY_THEN_ID)
                .toList();
    }

    public Optional<Ticket> findById(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    // A failed re-analysis keeps whatever insights the ticket already had: a transient
    // ai-service outage should not wipe out a good earlier analysis.
    public Optional<Ticket> reanalyze(Long id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            return Optional.empty();
        }
        aiServiceClient.analyze(ticket.getSubject(), ticket.getBody()).ifPresent(insights -> {
            ticket.setAi(insights);
            ticket.setAiStatus(AiStatus.ANALYZED);
        });
        // replace() rather than put() so a ticket deleted during the AI call is not resurrected.
        if (tickets.replace(id, ticket) == null) {
            return Optional.empty();
        }
        return Optional.of(ticket);
    }

    public Optional<Ticket> resolve(Long id) {
        return Optional.ofNullable(tickets.computeIfPresent(id, (key, ticket) -> {
            ticket.setStatus(TicketStatus.RESOLVED);
            return ticket;
        }));
    }

    public boolean delete(Long id) {
        return tickets.remove(id) != null;
    }

    public TicketStats stats() {
        List<Ticket> all = List.copyOf(tickets.values());
        List<AiInsights> insights = all.stream()
                .map(Ticket::getAi)
                .filter(Objects::nonNull)
                .toList();
        return new TicketStats(
                all.size(),
                all.stream().filter(t -> t.getStatus() == TicketStatus.OPEN).count(),
                all.stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED).count(),
                all.stream().filter(t -> t.getAiStatus() == AiStatus.UNAVAILABLE).count(),
                countBy(insights, AiInsights::category),
                countBy(insights, AiInsights::priority));
    }

    private static Map<String, Long> countBy(List<AiInsights> insights, Function<AiInsights, String> key) {
        return insights.stream()
                .filter(ai -> key.apply(ai) != null)
                .collect(Collectors.groupingBy(key, TreeMap::new, Collectors.counting()));
    }

    private static boolean matches(String filter, String value) {
        return filter == null || filter.isBlank() || filter.equalsIgnoreCase(value);
    }

    // Unanalyzed tickets and priorities this service does not recognise sort last.
    private static int priorityRank(Ticket ticket) {
        if (ticket.getAi() == null || ticket.getAi().priority() == null) {
            return PRIORITY_ORDER.size();
        }
        int rank = PRIORITY_ORDER.indexOf(ticket.getAi().priority().toUpperCase(Locale.ROOT));
        return rank < 0 ? PRIORITY_ORDER.size() : rank;
    }
}
