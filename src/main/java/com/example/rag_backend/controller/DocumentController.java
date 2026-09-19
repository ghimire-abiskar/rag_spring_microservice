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

    private final Path uploadDir = Paths.get("/app/uploads");
    private final RestTemplate restTemplate = new RestTemplate();
    private final String PYTHON_SERVICE_URL = "http://rag-python:8000/process";

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

            // Save file to shared Docker volume
            Files.copy(file.getInputStream(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

            // Notify Python service to extract text/PAN and rebuild vector DB
            Map<String, String> pythonPayload = Map.of(
                    "filePath", destinationPath.toString(),
                    "fileName", originalFilename
            );

            // Forward to Python container
            restTemplate.postForObject(PYTHON_SERVICE_URL, pythonPayload, Map.class);

            return ResponseEntity.ok(Map.of(
                    "message", "File '" + originalFilename + "' saved and ingestion triggered successfully."
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Disk write failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Ingestion trigger failed: " + e.getMessage()));
        }
    }
}