package com.example.mall.chat;

import com.example.mall.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat/send")
    public ApiResponse<ChatResponse> send(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.send(request));
    }

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@Valid @RequestBody ChatRequest request) {
        return chatService.stream(request);
    }

    @DeleteMapping("/api/chat/session/{sessionId}")
    public ApiResponse<Void> clear(@PathVariable String sessionId) {
        chatService.clear(sessionId);
        return ApiResponse.success(null);
    }
}
