package com.example.agentclient.controller;

import com.example.agentclient.service.ChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(
                request.message(),
                request.conversationId()
        );
        return new ChatResponse(response);
    }

    public record ChatRequest(
            String message,
            String conversationId
    ) {}

    public record ChatResponse(String response) {}
}
