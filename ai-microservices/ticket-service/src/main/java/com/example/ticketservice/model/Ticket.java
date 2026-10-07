package com.example.ticketservice.model;

import java.time.Instant;

public class Ticket {

    private Long id;

    private String subject;

    private String body;

    private String customerEmail;

    private TicketStatus status;

    private Instant createdAt;

    private AiInsights ai;

    private AiStatus aiStatus;

    public Ticket() {
    }

    public Ticket(Long id, String subject, String body, String customerEmail, Instant createdAt) {
        this.id = id;
        this.subject = subject;
        this.body = body;
        this.customerEmail = customerEmail;
        this.status = TicketStatus.OPEN;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AiInsights getAi() {
        return ai;
    }

    public void setAi(AiInsights ai) {
        this.ai = ai;
    }

    public AiStatus getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(AiStatus aiStatus) {
        this.aiStatus = aiStatus;
    }
}
