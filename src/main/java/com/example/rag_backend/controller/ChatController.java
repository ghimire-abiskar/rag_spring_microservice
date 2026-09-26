package com.example.rag_backend.controller;

import com.example.rag_backend.dto.ChatRequest;
import com.example.rag_backend.dto.ChatResponse;
import com.example.rag_backend.service.RagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;

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
    public ResponseEntity<ChatResponse> askQuestion(
            @RequestBody ChatRequest request,
            Principal principal) { // 1. Inject Principal

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    new ChatResponse("Question cannot be empty.", Collections.emptyList())
            );
        }

        try {
            // 2. Extract the verified user ID from the JWT
            String userId = principal.getName();

            // 3. Pass the userId to the service layer
            ChatResponse response = ragService.askQuestion(request, userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Python Engine Error: " + e.getMessage());
            return ResponseEntity.status(500).body(
                    new ChatResponse("Python Engine Error: " + e.getMessage(), Collections.emptyList())
            );
        }
    }
}