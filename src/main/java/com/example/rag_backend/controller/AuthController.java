package com.example.rag_backend.controller;

import com.example.rag_backend.entity.User;
import com.example.rag_backend.security.JwtUtil;
import com.example.rag_backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthService authService;
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.get("username"), loginRequest.get("password"))
            );
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "Incorrect username or password"));
        }

        final String jwt = jwtUtil.generateToken(loginRequest.get("username"));
        return ResponseEntity.ok(Map.of("token", jwt));
    }
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and password are required."));
        }

        try {
            User newUser = authService.registerUser(username, password);
            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully!",
                    "userId", newUser.getId(),
                    "username", newUser.getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}