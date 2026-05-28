package com.example.mall.chat;

import java.util.List;

public record AiChatResponse(List<Choice> choices) {

    public record Choice(ChatMessage message) {
    }
}
