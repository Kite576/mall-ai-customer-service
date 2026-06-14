package com.example.mall.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<ToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId) {

    public ChatMessage(String role, String content) {
        this(role, content, null, null);
    }

    public static ChatMessage toolResult(String toolCallId, String content) {
        return new ChatMessage("tool", content, null, toolCallId);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolCall(String id, String type, ToolFunction function) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolFunction(String name, String arguments) {
    }
}
