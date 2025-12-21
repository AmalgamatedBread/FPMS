package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.dto.ProfileUpdateRequest;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserContextService userContextService;
    private final AuthService authService;

    // Get upload directory from application.properties or use default
    @Value("${file.upload-dir:uploads}")
    private String UPLOAD_BASE_DIR;

    @Transactional
    public ResponseEntity<?> updateProfile(Long userId, ProfileUpdateRequest request) {
        try {
            // Find faculty by ID
            Faculty faculty = facultyRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update basic information
            if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
                faculty.setFirstName(request.getFirstName().trim());
            }
            if (request.getMiddleName() != null) {
                faculty.setMiddleName(request.getMiddleName().trim().isEmpty() ? null : request.getMiddleName().trim());
            }
            if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
                faculty.setLastName(request.getLastName().trim());
            }

            // FIX: Proper suffix handling - set to null if empty
            if (request.getSuffix() != null) {
                String suffix = request.getSuffix().trim();
                faculty.setSuffix(suffix.isEmpty() ? null : suffix);
            } else {
                faculty.setSuffix(null);
            }

            // Map contactNo to telNo
            if (request.getContactNo() != null) {
                faculty.setTelNo(request.getContactNo().trim().isEmpty() ? null : request.getContactNo().trim());
            }
            if (request.getAddress() != null) {
                faculty.setAddress(request.getAddress().trim().isEmpty() ? null : request.getAddress().trim());
            }

            faculty.setUpdatedAt(LocalDateTime.now());
            Faculty savedFaculty = facultyRepository.save(faculty);

            // Get username from credentials
            String username = "";
            if (savedFaculty.getSystemCredentials() != null) {
                username = savedFaculty.getSystemCredentials().getUsername();
            }

            // Get department name
            String departmentName = "";
            if (savedFaculty.getDepartment() != null) {
                departmentName = savedFaculty.getDepartment().getDeptName();
            }

            // Get profile photo URL
            String profilePhoto = getProfilePhotoUrl(userId);

            // Create response with updated data
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", savedFaculty.getFacultyId());
            userData.put("firstName", savedFaculty.getFirstName());
            userData.put("middleName", savedFaculty.getMiddleName() != null ? savedFaculty.getMiddleName() : "");
            userData.put("lastName", savedFaculty.getLastName());
            userData.put("suffix", savedFaculty.getSuffix() != null ? savedFaculty.getSuffix() : "");
            userData.put("email", savedFaculty.getEmail());
            userData.put("contactNo", savedFaculty.getTelNo() != null ? savedFaculty.getTelNo() : "");
            userData.put("address", savedFaculty.getAddress() != null ? savedFaculty.getAddress() : "");
            userData.put("username", username);
            userData.put("role", savedFaculty.getRole().name());
            userData.put("departmentName", departmentName);
            userData.put("profilePhoto", profilePhoto != null ? profilePhoto : "/icons/profile.png");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", userData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Profile update failed: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Profile update failed: " + e.getMessage());
            errorResponse.put("success", false);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Transactional
    public ResponseEntity<?> changePassword(Long userId, String currentPassword, String newPassword) {
        try {
            // Find credentials by faculty ID
            SystemCredentials credentials = credentialsRepository.findByFaculty_FacultyId(userId)
                    .orElseThrow(() -> new RuntimeException("User credentials not found"));

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, credentials.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
            }

            // Update to new password
            credentials.setPasswordHash(passwordEncoder.encode(newPassword));
            credentials.setUpdatedAt(LocalDateTime.now());
            credentialsRepository.save(credentials);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password changed successfully"
            ));

        } catch (Exception e) {
            log.error("Password change failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Password change failed: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getProfile(Long userId) {
        try {
            Faculty faculty = facultyRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get username from SystemCredentials
            String username = "";
            if (faculty.getSystemCredentials() != null) {
                username = faculty.getSystemCredentials().getUsername();
            }

            // Get department name if exists
            String departmentName = "";
            if (faculty.getDepartment() != null) {
                departmentName = faculty.getDepartment().getDeptName();
            }

            // Get profile photo URL with detailed logging
            String profilePhoto = getProfilePhotoUrl(userId);
            log.info("Profile photo URL for user {}: {}", userId, profilePhoto);

            // Use HashMap for user data
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", faculty.getFacultyId());
            userMap.put("firstName", faculty.getFirstName());
            userMap.put("middleName", faculty.getMiddleName() != null ? faculty.getMiddleName() : "");
            userMap.put("lastName", faculty.getLastName());
            userMap.put("suffix", faculty.getSuffix() != null ? faculty.getSuffix() : "");
            userMap.put("email", faculty.getEmail());
            userMap.put("contactNo", faculty.getTelNo() != null ? faculty.getTelNo() : "");
            userMap.put("address", faculty.getAddress() != null ? faculty.getAddress() : "");
            userMap.put("role", faculty.getRole().name());
            userMap.put("username", username);
            userMap.put("departmentName", departmentName);
            userMap.put("createdAt", faculty.getCreatedAt());
            userMap.put("updatedAt", faculty.getUpdatedAt());
            userMap.put("profilePhoto", profilePhoto);

            log.info("Returning profile data for user {}: {}", userId, userMap);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userMap);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch profile: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch profile: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> updateEmail(Long userId, String newEmail, String password) {
        try {
            // Verify password first
            SystemCredentials credentials = credentialsRepository.findByFaculty_FacultyId(userId)
                    .orElseThrow(() -> new RuntimeException("User credentials not found"));

            if (!passwordEncoder.matches(password, credentials.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is incorrect"));
            }

            // Check if email is already taken
            if (facultyRepository.findByEmail(newEmail).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
            }

            // Update email
            Faculty faculty = credentials.getFaculty();
            faculty.setEmail(newEmail);
            faculty.setUpdatedAt(LocalDateTime.now());
            facultyRepository.save(faculty);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email updated successfully",
                    "newEmail", newEmail
            ));

        } catch (Exception e) {
            log.error("Email update failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Email update failed: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<?> uploadProfilePhoto(Long userId, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 5MB limit"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.equals("image/jpeg") &&
                            !contentType.equals("image/png") &&
                            !contentType.equals("image/gif"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only JPEG, PNG, and GIF images are allowed"));
            }

            // Get user's profile photos directory
            String userProfileDir = getUserProfilePhotosDirectory(userId);

            // DEBUG: Show where we're trying to save
            log.info("Attempting to save to directory: {}", userProfileDir);

            Path uploadPath = Paths.get(userProfileDir);
            log.info("Absolute path: {}", uploadPath.toAbsolutePath());

            // Create directories if they don't exist
            Files.createDirectories(uploadPath);
            log.info("Created directories if they didn't exist");

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String filename = "profile_" + System.currentTimeMillis() + fileExtension;
            Path filePath = uploadPath.resolve(filename);

            // Save file
            long bytesWritten = Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Profile photo saved: {} ({} bytes)", filePath.toAbsolutePath(), bytesWritten);

            // Get the URL for the uploaded file
            String photoUrl = "/uploads/users/" + userId + "/profile-photos/" + filename;
            log.info("Profile photo URL: {}", photoUrl);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile photo uploaded successfully",
                    "filename", filename,
                    "photoUrl", photoUrl,
                    "fileSize", bytesWritten
            ));

        } catch (Exception e) {
            log.error("Photo upload failed: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Photo upload failed: " + e.getMessage()));
        }
    }

    /**
     * Get user's profile photos directory
     */
    public String getUserProfilePhotosDirectory(Long userId) {
        return UPLOAD_BASE_DIR + "/users/" + userId + "/profile-photos/";
    }

    /**
     * Get the most recent profile photo URL for a user
     */
    private String getProfilePhotoUrl(Long userId) {
        try {
            String userProfileDir = getUserProfilePhotosDirectory(userId);
            Path uploadPath = Paths.get(userProfileDir);

            log.info("Looking for profile photos for user {} at: {}", userId, uploadPath.toAbsolutePath());

            if (!Files.exists(uploadPath)) {
                log.info("Directory does not exist for user {}", userId);
                return "/icons/profile.png";
            }

            // Get all image files
            List<Path> imageFiles = Files.list(uploadPath)
                    .filter(path -> {
                        String filename = path.getFileName().toString().toLowerCase();
                        return filename.endsWith(".jpg") || filename.endsWith(".jpeg") ||
                                filename.endsWith(".png") || filename.endsWith(".gif");
                    })
                    .collect(Collectors.toList());

            log.info("Found {} image files for user {}", imageFiles.size(), userId);

            if (!imageFiles.isEmpty()) {
                // Get the most recent file
                Path latestFile = imageFiles.stream()
                        .max((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .orElse(null);

                if (latestFile != null) {
                    String filename = latestFile.getFileName().toString();
                    String photoUrl = "/uploads/users/" + userId + "/profile-photos/" + filename;
                    log.info("Selected profile photo: {}", photoUrl);

                    // Verify file exists and is readable
                    if (Files.exists(latestFile) && Files.isReadable(latestFile)) {
                        long fileSize = Files.size(latestFile);
                        log.info("File verified - size: {} bytes", fileSize);
                        return photoUrl;
                    }
                }
            }

            log.info("No valid profile photo found, using default");
            return "/icons/profile.png";

        } catch (Exception e) {
            log.error("Error getting profile photo: ", e);
            return "/icons/profile.png";
        }
    }

    /**
     * Delete old profile photos for a user (keep only the latest)
     */
    @Transactional
    public ResponseEntity<?> cleanupProfilePhotos(Long userId) {
        try {
            String userProfileDir = getUserProfilePhotosDirectory(userId);
            Path uploadPath = Paths.get(userProfileDir);

            if (!Files.exists(uploadPath)) {
                return ResponseEntity.ok(Map.of("success", true, "message", "No photos to clean up"));
            }

            // Get all profile photos sorted by modification time (newest first)
            var photos = Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("profile_"))
                    .sorted((p1, p2) -> {
                        try {
                            return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .toList();

            // Delete all except the most recent one
            int deletedCount = 0;
            for (int i = 1; i < photos.size(); i++) {
                try {
                    Files.delete(photos.get(i));
                    deletedCount++;
                } catch (IOException e) {
                    log.error("Failed to delete old photo: {}", photos.get(i).getFileName(), e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleaned up " + deletedCount + " old profile photos");
            response.put("kept", photos.size() > 0 ? 1 : 0);
            response.put("deleted", deletedCount);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error cleaning up profile photos: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to clean up photos: " + e.getMessage()));
        }
    }
}