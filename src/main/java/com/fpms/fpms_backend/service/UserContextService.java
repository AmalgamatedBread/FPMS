package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserContextService {

    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository credentialsRepository;

    /**
     * Get the current authenticated user's ID
     */
    public Optional<Long> getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            String username;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else {
                username = principal.toString();
            }

            Optional<SystemCredentials> credentials = credentialsRepository.findByUsername(username);
            return credentials.map(cred -> cred.getFaculty().getFacultyId());

        } catch (Exception e) {
            log.error("Error getting current user ID: ", e);
            return Optional.empty();
        }
    }

    /**
     * Get the current authenticated faculty entity
     */
    public Optional<Faculty> getCurrentFaculty() {
        try {
            Optional<Long> userId = getCurrentUserId();
            if (userId.isEmpty()) {
                return Optional.empty();
            }
            return facultyRepository.findById(userId.get());
        } catch (Exception e) {
            log.error("Error getting current faculty: ", e);
            return Optional.empty();
        }
    }

    /**
     * Get the current authenticated user's username
     */
    public Optional<String> getCurrentUsername() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return Optional.of(((UserDetails) principal).getUsername());
            } else {
                return Optional.of(principal.toString());
            }
        } catch (Exception e) {
            log.error("Error getting current username: ", e);
            return Optional.empty();
        }
    }
}