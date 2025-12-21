package com.fpms.fpms_backend.security;

import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
public class CustomUserDetails implements UserDetails {
    private final SystemCredentials credentials;
    private final Faculty faculty;

    public CustomUserDetails(SystemCredentials credentials) {
        this.credentials = credentials;
        this.faculty = credentials.getFaculty();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = faculty.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return credentials.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return credentials.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Custom getters for faculty properties
    public String getFirstName() {
        return faculty.getFirstName();
    }

    public String getMiddleName() {
        return faculty.getMiddleName();
    }

    public String getLastName() {
        return faculty.getLastName();
    }

    public String getSuffix() {
        return faculty.getSuffix();
    }

    public String getEmail() {
        return faculty.getEmail();
    }

    public String getTelNo() {
        return faculty.getTelNo();
    }

    public String getAddress() {
        return faculty.getAddress();
    }

    public String getRole() {
        return faculty.getRole().name();
    }

    public Long getFacultyId() {
        return faculty.getFacultyId();
    }

    // Get department name if available
    public String getDepartmentName() {
        if (faculty.getDepartment() != null) {
            return faculty.getDepartment().getDeptName();
        }
        return "No Department";
    }

    // Get department code if available
    public String getDepartmentCode() {
        if (faculty.getDepartment() != null) {
            return faculty.getDepartment().getDeptCode();
        }
        return null;
    }
}