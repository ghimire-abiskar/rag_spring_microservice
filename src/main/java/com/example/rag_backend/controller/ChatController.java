package com.example.rag_backend.controller;

import com.example.rag_backend.dto.ChatRequest;
import com.example.rag_backend.dto.ChatResponse;
import com.example.rag_backend.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("Spring Boot Gateway is online and ready!");
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> askQuestion(@RequestBody ChatRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ChatResponse("Question cannot be empty.", java.util.Collections.emptyList())
            );
        }

        ChatResponse response = ragService.processQuery(request);
        return ResponseEntity.ok(response);
    }
}