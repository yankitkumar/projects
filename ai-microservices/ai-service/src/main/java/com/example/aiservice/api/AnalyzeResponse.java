package com.example.aiservice.api;

import com.example.aiservice.analysis.Category;
import com.example.aiservice.analysis.Priority;
import com.example.aiservice.analysis.Sentiment;
import com.example.aiservice.analysis.TicketAnalysis;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyzeResponse(
        Category category,
        Priority priority,
        Sentiment sentiment,
        String summary,
        String suggestedReply,
        String engine,
        String fallbackReason) {

    public static AnalyzeResponse of(TicketAnalysis analysis, String engine, String fallbackReason) {
        return new AnalyzeResponse(analysis.category(), analysis.priority(), analysis.sentiment(),
                analysis.summary(), analysis.suggestedReply(), engine, fallbackReason);
    }
}
