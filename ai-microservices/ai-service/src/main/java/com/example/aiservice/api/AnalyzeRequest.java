package com.example.aiservice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalyzeRequest(
        @NotBlank(message = "subject must not be blank")
        @Size(max = 200, message = "subject must be at most 200 characters")
        String subject,
        @NotBlank(message = "body must not be blank")
        @Size(max = 5000, message = "body must be at most 5000 characters")
        String body) {
}
