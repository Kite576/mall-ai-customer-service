package com.example.mall.chat;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(@NotBlank String message, String sessionId, List<ChatMessage> context) {
}
