package com.fpms.fpms_backend.controller;

import com.fpms.fpms_backend.dto.ProfileUpdateRequest;
import com.fpms.fpms_backend.service.ProfileService;
import com.fpms.fpms_backend.service.UserContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final UserContextService userContextService;

    // Inject the same upload directory as ProfileService
    @Value("${file.upload-dir:uploads}")
    private String uploadBaseDir;

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            log.info("Fetching profile for user ID: {}", userId);
            return profileService.getProfile(userId);

        } catch (Exception e) {
            log.error("Failed to fetch profile: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            log.info("Updating profile for user ID: {}", userId);
            return profileService.updateProfile(userId, request);

        } catch (Exception e) {
            log.error("Profile update failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordRequest) {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            String currentPassword = passwordRequest.get("currentPassword");
            String newPassword = passwordRequest.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Both current and new password are required"));
            }

            log.info("Changing password for user ID: {}", userId);
            return profileService.changePassword(userId, currentPassword, newPassword);

        } catch (Exception e) {
            log.error("Password change failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update-email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> emailRequest) {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            String newEmail = emailRequest.get("newEmail");
            String password = emailRequest.get("password");

            if (newEmail == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Both new email and password are required"));
            }

            log.info("Updating email for user ID: {}", userId);
            return profileService.updateEmail(userId, newEmail, password);

        } catch (Exception e) {
            log.error("Email update failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            log.info("Uploading profile photo for user ID: {}", userId);
            return profileService.uploadProfilePhoto(userId, file);

        } catch (Exception e) {
            log.error("Photo upload failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cleanup-photos")
    public ResponseEntity<?> cleanupProfilePhotos() {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            log.info("Cleaning up profile photos for user ID: {}", userId);
            return profileService.cleanupProfilePhotos(userId);

        } catch (Exception e) {
            log.error("Photo cleanup failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FPMS Profile Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ===== DEBUG ENDPOINTS =====

    @GetMapping("/debug-upload-path")
    public ResponseEntity<?> debugUploadPath() {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            // Get the expected upload path using consistent method
            String userProfileDir = getUserProfilePhotosDirectory(userId);
            Path uploadPath = Paths.get(userProfileDir).toAbsolutePath().normalize();

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("userId", userId);
            debugInfo.put("expectedPath", userProfileDir);
            debugInfo.put("absolutePath", uploadPath.toString());
            debugInfo.put("exists", Files.exists(uploadPath));
            debugInfo.put("isDirectory", Files.isDirectory(uploadPath));
            debugInfo.put("baseDirConfig", uploadBaseDir);

            // Try to create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                debugInfo.put("created", true);
            }

            // Check uploads base directory
            Path basePath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
            debugInfo.put("basePath", basePath.toString());
            debugInfo.put("baseExists", Files.exists(basePath));

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("Debug error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/test-static-config")
    public ResponseEntity<?> testStaticConfig() {
        try {
            Long userId = userContextService.getCurrentUserId()
                    .orElseThrow(() -> new RuntimeException("User not authenticated"));

            Map<String, Object> result = new HashMap<>();

            // Check the directory
            String userProfileDir = getUserProfilePhotosDirectory(userId);
            Path dirPath = Paths.get(userProfileDir).toAbsolutePath().normalize();
            result.put("dirAbsolutePath", dirPath.toString());
            result.put("dirExists", Files.exists(dirPath));

            // List files in directory
            if (Files.exists(dirPath)) {
                var files = Files.list(dirPath)
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
                result.put("files", files);
                result.put("fileCount", files.size());
            }

            // Test URL
            result.put("testUploadUrl", "http://localhost:8080/uploads/users/" + userId + "/profile-photos/test.jpg");
            result.put("configUploadDir", uploadBaseDir);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get user profile photos directory - using same logic as ProfileService
     */
    public String getUserProfilePhotosDirectory(Long userId) {
        // Ensure consistent path with ProfileService
        String baseDir = uploadBaseDir.endsWith("/") ? uploadBaseDir : uploadBaseDir + "/";
        return baseDir + "users/" + userId + "/profile-photos/";
    }
}