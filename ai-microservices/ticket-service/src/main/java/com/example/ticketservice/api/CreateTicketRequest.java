package com.example.ticketservice.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Limits and messages mirror ai-service's analyze contract so a valid ticket is never rejected downstream.
public record CreateTicketRequest(
        @NotBlank(message = "subject must not be blank")
        @Size(max = 200, message = "subject must be at most 200 characters")
        String subject,
        @NotBlank(message = "body must not be blank")
        @Size(max = 5000, message = "body must be at most 5000 characters")
        String body,
        @Email(message = "customerEmail must be a valid email address")
        @Size(max = 254, message = "customerEmail must be at most 254 characters")
        String customerEmail) {
}
