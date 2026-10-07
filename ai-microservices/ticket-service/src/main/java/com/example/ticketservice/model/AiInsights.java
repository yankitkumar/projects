package com.example.ticketservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Plain Strings rather than enums, and unknown fields ignored, so ticket-service keeps
// working when ai-service adds a new category, priority or response field.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiInsights(
        String category,
        String priority,
        String sentiment,
        String summary,
        String suggestedReply,
        String engine) {
}
