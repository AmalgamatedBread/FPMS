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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

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

        // Create response
        return createAuthResponse(savedFaculty, savedCredentials);
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

        // Also update credentials account type - FIXED METHOD CALL
        SystemCredentials credentials = credentialsRepository.findByFaculty_FacultyId(facultyId)
                .orElseThrow(() -> new RuntimeException("Credentials not found for faculty ID: " + facultyId));
        credentials.setAccountType(newRole.toString());
        credentialsRepository.save(credentials);

        log.info("Updated role for faculty ID {} to {}", facultyId, newRole);
    }
}