package com.example.aiservice.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

// Also the structured-output schema sent to Claude, so the descriptions are written for the model.
public record TicketAnalysis(
        @JsonPropertyDescription("The area of the product the ticket is about")
        Category category,
        @JsonPropertyDescription("How urgently support should respond")
        Priority priority,
        @JsonPropertyDescription("The customer's overall tone")
        Sentiment sentiment,
        @JsonPropertyDescription("One-sentence summary of the customer's issue, at most 25 words")
        String summary,
        @JsonPropertyDescription("A short, polite first reply to the customer (2-4 sentences) that promises no refunds or timelines")
        String suggestedReply) {
}
