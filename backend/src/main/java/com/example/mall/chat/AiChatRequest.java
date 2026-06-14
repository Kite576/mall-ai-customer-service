package com.example.mall.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiChatRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Integer max_tokens,
        Boolean stream,
        List<Tool> tools,
        Object tool_choice) {

    public AiChatRequest(String model, List<ChatMessage> messages, Double temperature, Integer max_tokens, Boolean stream) {
        this(model, messages, temperature, max_tokens, stream, null, null);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tool(String type, Function function) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Function(String name, String description, Map<String, Object> parameters) {
    }
}
