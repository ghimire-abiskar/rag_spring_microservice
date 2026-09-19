package com.example.rag_backend.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    // Defaults to a relative "uploads" folder in your project directory when running locally
    private final Path uploadDir = Paths.get(System.getenv().getOrDefault("UPLOAD_DIR", "uploads"));
    private final RestTemplate restTemplate;

    // Defaults to localhost for local testing, can be overridden in Docker
    private final String PYTHON_SERVICE_URL = System.getenv().getOrDefault("PYTHON_URL", "http://13.49.138.96:8000/process");

    public DocumentController(RestTemplate restTemplate) throws IOException {
        this.restTemplate = restTemplate;
        Files.createDirectories(uploadDir);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty."));
        }

        try {
            // 1. Save locally to Spring
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path destinationPath = uploadDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // 2. Forward the actual file bytes to the Python microservice
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            try {
                restTemplate.postForEntity(PYTHON_SERVICE_URL, requestEntity, String.class);
            } catch (Exception e) {
                System.out.println("Python Engine Error: " + e.getMessage());
                return ResponseEntity.status(500).body(Map.of("message", "Python Engine Error: " + e.getMessage()));
            }

            return ResponseEntity.ok(Map.of("message", "File '" + originalFilename + "' saved and ingested successfully."));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Disk write failed: " + e.getMessage()));
        }
    }
}