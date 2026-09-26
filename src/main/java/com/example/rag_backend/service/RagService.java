package com.example.rag_backend.service;

import com.example.rag_backend.dto.ChatRequest;
import com.example.rag_backend.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Service
public class RagService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    public String processDocument(MultipartFile file, String userId) throws IOException {
        byte[] fileBytes = file.getBytes();
        String originalFilename = Paths.get(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
        ).getFileName().toString();

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return originalFilename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);
        body.add("user_id", userId); // FastAPI Form(...) parameter

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                pythonServiceUrl + "/process",
                entity,
                String.class
        );
        return response.getBody();
    }

    public ChatResponse askQuestion(ChatRequest request, String userId) {
        Map<String, String> pythonPayload = new HashMap<>();
        pythonPayload.put("question", request.getQuestion());
        pythonPayload.put("user_id", userId); // FastAPI Pydantic field

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(pythonPayload, headers);

        ResponseEntity<ChatResponse> response = restTemplate.postForEntity(
                pythonServiceUrl + "/api/query",
                entity,
                ChatResponse.class
        );
        return response.getBody();
    }
}