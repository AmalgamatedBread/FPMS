package com.fpms.fpms_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 1, message = "First name cannot be empty")
    private String firstName;

    private String middleName;

    @Size(min = 1, message = "Last name cannot be empty")
    private String lastName;

    private String suffix;

    @Email(message = "Invalid email format")
    private String email;

    private String contactNo;

    private String address;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}