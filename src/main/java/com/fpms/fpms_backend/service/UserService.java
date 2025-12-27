package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.dto.AuthResponse;
import com.fpms.fpms_backend.dto.RegisterRequest;
import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import com.fpms.fpms_backend.repository.DepartmentRepository;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    // Upload directory
    private final String UPLOAD_BASE_DIR = "uploads";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user via UserService: {}", request.getUsername());

        // Check if username already exists
        if (credentialsRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (facultyRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Parse role
        FacultyRole role;
        try {
            role = FacultyRole.valueOf(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + request.getRole());
        }

        // Get department if specified
        Department department = null;
        if (request.getDepartment() != null && !request.getDepartment().isEmpty()) {
            department = departmentRepository.findByDeptCode(request.getDepartment())
                    .orElseThrow(() -> new RuntimeException("Department not found: " + request.getDepartment()));
        }

        // Create faculty entity
        Faculty faculty = Faculty.builder()
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .suffix(request.getSuffix())
                .email(request.getEmail())
                .telNo(request.getTelNo())
                .address(request.getAddress())
                .role(role)
                .department(department)
                .build();

        // Save faculty
        Faculty savedFaculty = facultyRepository.save(faculty);
        log.info("Faculty created with ID: {}", savedFaculty.getFacultyId());

        // Create credentials
        SystemCredentials credentials = SystemCredentials.builder()
                .faculty(savedFaculty)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .accountType(role.toString())
                .build();

        // Save credentials
        SystemCredentials savedCredentials = credentialsRepository.save(credentials);
        log.info("Credentials created for faculty ID: {}", savedFaculty.getFacultyId());

        // Associate credentials with faculty
        savedFaculty.setSystemCredentials(savedCredentials);
        facultyRepository.save(savedFaculty);

        // Create upload directory for the new user
        try {
            createUserUploadDirectory(savedFaculty.getFacultyId());
            log.info("Upload directory created for user ID: {}", savedFaculty.getFacultyId());
        } catch (Exception e) {
            log.warn("Failed to create upload directory for user {}: {}", savedFaculty.getFacultyId(), e.getMessage());
            // Continue even if directory creation fails
        }

        // Create response
        return createAuthResponse(savedFaculty, savedCredentials);
    }

    /**
     * Creates upload directories for a user
     */
    private void createUserUploadDirectory(Long userId) {
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

    private AuthResponse createAuthResponse(Faculty faculty, SystemCredentials credentials) {
        AuthResponse.UserData userData = AuthResponse.UserData.builder()
                .facultyId(faculty.getFacultyId())
                .firstName(faculty.getFirstName())
                .middleName(faculty.getMiddleName())
                .lastName(faculty.getLastName())
                .suffix(faculty.getSuffix())
                .role(faculty.getRole())
                .email(faculty.getEmail())
                .telNo(faculty.getTelNo())
                .address(faculty.getAddress())
                .username(credentials.getUsername())
                .createdAt(faculty.getCreatedAt())
                .updatedAt(faculty.getUpdatedAt())
                .build();

        // Add department info if available
        if (faculty.getDepartment() != null) {
            userData.setDeptCode(faculty.getDepartment().getDeptCode());
            userData.setDeptName(faculty.getDepartment().getDeptName());
        }

        return AuthResponse.builder()
                .message("Registration successful")
                .token("temp-token-will-be-replaced-with-jwt")
                .user(userData)
                .build();
    }

    public Faculty getFacultyById(Long facultyId) {
        return facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with ID: " + facultyId));
    }

    public Faculty getFacultyByUsername(String username) {
        return facultyRepository.findBySystemCredentialsUsername(username)
                .orElseThrow(() -> new RuntimeException("Faculty not found with username: " + username));
    }

    public boolean existsByEmail(String email) {
        return facultyRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return credentialsRepository.existsByUsername(username);
    }

    @Transactional
    public void updateFacultyRole(Long facultyId, FacultyRole newRole) {
        Faculty faculty = getFacultyById(facultyId);
        faculty.setRole(newRole);
        facultyRepository.save(faculty);

        // Also update credentials account type
        SystemCredentials credentials = credentialsRepository.findByFaculty_FacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Credentials not found for faculty ID: " + facultyId));
        credentials.setAccountType(newRole.toString());
        credentialsRepository.save(credentials);

        log.info("Updated role for faculty ID {} to {}", facultyId, newRole);
    }
}
