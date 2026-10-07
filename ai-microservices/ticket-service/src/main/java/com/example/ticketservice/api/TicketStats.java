package com.example.ticketservice.api;

import java.util.Map;

public record TicketStats(
        long total,
        long open,
        long resolved,
        long unanalyzed,
        Map<String, Long> byCategory,
        Map<String, Long> byPriority) {
}
