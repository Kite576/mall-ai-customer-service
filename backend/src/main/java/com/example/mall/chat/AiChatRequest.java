package com.example.mall.chat;

import java.util.List;

public record AiChatRequest(String model, List<ChatMessage> messages, Double temperature, Integer max_tokens, Boolean stream) {
}
