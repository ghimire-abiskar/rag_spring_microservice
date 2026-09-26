package com.example.rag_backend.controller;

import org.springframework.core.io.ByteArrayResource;
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
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final Path uploadDir = Paths.get(System.getenv().getOrDefault("UPLOAD_DIR", "uploads"));
    private final RestTemplate restTemplate;

    // Points directly to your deployed FastAPI /process route
    private final String PYTHON_SERVICE_URL = System.getenv().getOrDefault("PYTHON_URL", "http://13.49.138.96:8000/process");

    public DocumentController(RestTemplate restTemplate) throws IOException {
        this.restTemplate = restTemplate;
        Files.createDirectories(uploadDir);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            Principal principal) { // 1. Inject Principal to get the authenticated JWT user

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty."));
        }

        try {
            // Extract the user ID from the JWT token
            String userId = principal.getName();

            // Save locally to Spring
            String originalFilename = Paths.get(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "document"
            ).getFileName().toString();

            Path destinationPath = uploadDir.resolve(originalFilename);
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // 2. Apply the ByteArrayResource fix to prevent RestTemplate boundary corruption
            byte[] fileBytes = file.getBytes();
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };

            // 3. Add BOTH the safe file resource and the user_id to the Python request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);
            body.add("user_id", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            try {
                restTemplate.postForEntity(PYTHON_SERVICE_URL, requestEntity, String.class);
            } catch (Exception e) {
                System.out.println("Python Engine Error: " + e.getMessage());
                return ResponseEntity.status(500).body(Map.of("message", "Python Engine Error: " + e.getMessage()));
            }

            return ResponseEntity.ok(Map.of("message", "File '" + originalFilename + "' saved and ingested securely for user: " + userId));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Disk write failed: " + e.getMessage()));
        }
    }
}