package com.example.mall.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message, String sessionId) {
}
