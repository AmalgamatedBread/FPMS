package com.fpms.fpms_backend.controller;

import com.fpms.fpms_backend.dto.AuthResponse;
import com.fpms.fpms_backend.dto.LoginRequest;
import com.fpms.fpms_backend.dto.RegisterRequest;
import com.fpms.fpms_backend.service.AuthService;
import com.fpms.fpms_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService; // Add UserService

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            log.info("Registration request for: {} with username: {}", request.getEmail(), request.getUsername());
            return authService.register(request);
        } catch (Exception e) {
            log.error("Registration error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());
            return authService.login(request);
        } catch (Exception e) {
            log.error("Login error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session")
    public ResponseEntity<?> createSession(@RequestBody Map<String, Object> userData) {
        try {
            log.info("Creating session for user: {}", userData.get("email"));
            return ResponseEntity.ok(Map.of("success", true, "message", "Session created"));
        } catch (Exception e) {
            log.error("Session creation error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            log.info("Logout request");
            return authService.logout();
        } catch (Exception e) {
            log.error("Logout error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession() {
        try {
            log.info("Checking session");
            return authService.checkSession();
        } catch (Exception e) {
            log.error("Session check error: ", e);
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    @PostMapping("/create-user-directories")
    public ResponseEntity<?> createUserDirectories() {
        try {
            authService.createUploadDirectoryForExistingUsers();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Upload directories created for all users"
            ));
        } catch (Exception e) {
            log.error("Failed to create user directories: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user-upload-info/{userId}")
    public ResponseEntity<?> getUserUploadInfo(@PathVariable Long userId) {
        try {
            String uploadDir = authService.getUserUploadDirectory(userId);
            String profileDir = authService.getUserProfilePhotosDirectory(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId,
                    "uploadDirectory", uploadDir,
                    "profilePhotosDirectory", profileDir
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FPMS Authentication Service",
                "timestamp", System.currentTimeMillis()
        ));
    }
}

