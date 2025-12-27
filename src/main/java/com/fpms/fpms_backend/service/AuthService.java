package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.dto.LoginRequest;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentService departmentService;
    private final AuthenticationManager authenticationManager;

    // Upload directory
    private final String UPLOAD_BASE_DIR = "uploads";

    @Transactional
    public ResponseEntity<?> login(LoginRequest request) {
        try {
            log.info("Login attempt for username: {}", request.getUsername());

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get user details
            Optional<SystemCredentials> credentials = credentialsRepository.findByUsername(request.getUsername());
            if (credentials.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Faculty faculty = credentials.get().getFaculty();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful",
                    "userId", faculty.getFacultyId(),
                    "username", request.getUsername(),
                    "role", faculty.getRole().name(),
                    "firstName", faculty.getFirstName(),
                    "lastName", faculty.getLastName(),
                    "email", faculty.getEmail()
            ));

        } catch (Exception e) {
            log.error("Login error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid username or password"));
        }
    }

    public ResponseEntity<?> logout() {
        try {
            SecurityContextHolder.clearContext();
            log.info("User logged out successfully");
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout successful"
            ));
        } catch (Exception e) {
            log.error("Logout error: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public ResponseEntity<?> checkSession() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return ResponseEntity.ok(Map.of(
                        "authenticated", true,
                        "username", auth.getName()
                ));
            }
            return ResponseEntity.ok(Map.of("authenticated", false));
        } catch (Exception e) {
            log.error("Session check error: ", e);
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    /**
     * Creates upload directories for a new user
     */
    public void createUserUploadDirectory(Long userId) {
        try {
            log.info("Creating upload directories for user ID: {}", userId);

            // Create base uploads directory if it doesn't exist
            Path baseDir = Paths.get(UPLOAD_BASE_DIR);
            if (!Files.exists(baseDir)) {
                Files.createDirectories(baseDir);
                log.info("Created base upload directory: {}", baseDir.toAbsolutePath());
            }

            // Create users directory
            Path usersDir = Paths.get(UPLOAD_BASE_DIR + "/users");
            if (!Files.exists(usersDir)) {
                Files.createDirectories(usersDir);
                log.info("Created users directory: {}", usersDir.toAbsolutePath());
            }

            // Create user-specific directories
            String[] directories = {
                    UPLOAD_BASE_DIR + "/users/" + userId,
                    UPLOAD_BASE_DIR + "/users/" + userId + "/profile-photos",
                    UPLOAD_BASE_DIR + "/users/" + userId + "/documents",
                    UPLOAD_BASE_DIR + "/users/" + userId + "/portfolios"
            };

            for (String dir : directories) {
                Path path = Paths.get(dir);
                if (!Files.exists(path)) {
                    Files.createDirectories(path);
                    log.info("Created directory: {}", path.toAbsolutePath());
                }
            }

            log.info("Upload directories created successfully for user ID: {}", userId);

        } catch (IOException e) {
            log.error("Failed to create upload directories for user {}: {}", userId, e.getMessage());
            // Don't throw exception - registration should still succeed even if dirs fail
        }
    }

    /**
     * Creates upload directories for all existing users
     */
    @Transactional
    public void createUploadDirectoryForExistingUsers() {
        try {
            log.info("Creating upload directories for all existing users...");

            List<Faculty> allFaculty = facultyRepository.findAll();
            int createdCount = 0;

            for (Faculty faculty : allFaculty) {
                Long userId = faculty.getFacultyId();
                try {
                    createUserUploadDirectory(userId);
                    createdCount++;
                } catch (Exception e) {
                    log.error("Failed to create directories for user {}: {}", userId, e.getMessage());
                }
            }

            log.info("Created upload directories for {} users", createdCount);

        } catch (Exception e) {
            log.error("Error creating directories for existing users: ", e);
            throw new RuntimeException("Failed to create directories for existing users: " + e.getMessage());
        }
    }

    /**
     * Get user's upload directory path
     */
    public String getUserUploadDirectory(Long userId) {
        return UPLOAD_BASE_DIR + "/users/" + userId;
    }

    /**
     * Get user's profile photos directory path
     */
    public String getUserProfilePhotosDirectory(Long userId) {
        return UPLOAD_BASE_DIR + "/users/" + userId + "/profile-photos";
    }

    @Transactional
    public Optional<Faculty> findByUsername(String username) {
        return facultyRepository.findBySystemCredentialsUsername(username);
    }

    @Transactional
    public Optional<SystemCredentials> findCredentialsByUsername(String username) {
        return credentialsRepository.findByUsername(username);
    }
}
