package com.fpms.fpms_backend.dto;

import com.fpms.fpms_backend.model.enums.FacultyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String token;
    private UserData user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserData {
        private Long facultyId;
        private String firstName;
        private String middleName;
        private String lastName;
        private String suffix;
        private FacultyRole role;
        private String email;
        private String telNo;
        private String address;
        private String username;
        private String deptCode;
        private String deptName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
