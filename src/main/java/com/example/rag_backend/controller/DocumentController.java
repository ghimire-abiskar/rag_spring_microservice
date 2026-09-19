package com.example.rag_backend.controller;

import org.springframework.http.ResponseEntity;
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
    private final RestTemplate restTemplate = new RestTemplate();

    // Defaults to localhost for local testing, can be overridden in Docker
    private final String PYTHON_SERVICE_URL = System.getenv().getOrDefault("PYTHON_URL", "http://13.49.138.96:8000/process");

    public DocumentController() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty."));
        }

        try {
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
            Path destinationPath = uploadDir.resolve(originalFilename);

            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            Map<String, String> pythonPayload = Map.of(
                    "filePath", destinationPath.toString(),
                    "fileName", originalFilename
            );

            // Optional: Catch RestClientException locally since Python isn't running yet
            try {
                restTemplate.postForObject(PYTHON_SERVICE_URL, pythonPayload, Map.class);
            } catch (Exception e) {
                System.out.println("Warning: Could not reach Python service. File saved locally to " + destinationPath);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "File '" + originalFilename + "' saved successfully."
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Disk write failed: " + e.getMessage()));
        }
    }
}