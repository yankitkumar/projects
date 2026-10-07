package com.example.aiservice.analysis;

public interface TicketAnalyzer {

    TicketAnalysis analyze(String subject, String body);
}
