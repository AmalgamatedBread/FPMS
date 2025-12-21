package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import com.fpms.fpms_backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SystemCredentialsRepository systemCredentialsRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SystemCredentials credentials = systemCredentialsRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (credentials.getFaculty() == null) {
            throw new UsernameNotFoundException("Faculty not found for user: " + username);
        }

        return new CustomUserDetails(credentials); // Return your custom implementation
    }
}