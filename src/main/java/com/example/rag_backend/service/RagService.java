package com.example.rag_backend.service;

import com.example.rag_backend.dto.ChatRequest;
import com.example.rag_backend.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RagService {

    private final RestTemplate restTemplate;

    @Value("${python.rag.service.url:http://localhost:8000/api/query}")
    private String pythonServiceUrl;

    public RagService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ChatResponse processQuery(ChatRequest request) {
        try {
            // Forward the question to the Python FastAPI microservice
            return restTemplate.postForObject(pythonServiceUrl, request, ChatResponse.class);
        } catch (Exception e) {
            // Graceful fallback if the Python microservice is offline
            return new ChatResponse(
                    "Error communicating with AI engine: " + e.getMessage(),
                    java.util.Collections.emptyList()
            );
        }
    }
}