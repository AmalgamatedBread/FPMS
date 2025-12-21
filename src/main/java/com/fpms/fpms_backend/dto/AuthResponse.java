package com.fpms.fpms_backend.dto;

import com.fpms.fpms_backend.model.enums.FacultyRole;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String token;
    private UserData user;

    @Getter
    @Setter
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
        private String deptCode;
        private String deptName;
        private String username;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Helper methods for formatted dates
        public String getFormattedCreatedAt() {
            if (createdAt == null) return "Unknown";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return createdAt.format(formatter);
        }

        public String getFormattedUpdatedAt() {
            if (updatedAt == null) return "Never updated";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
            return updatedAt.format(formatter);
        }

        // Helper method for full name display
        public String getFullName() {
            StringBuilder fullName = new StringBuilder(firstName);
            if (middleName != null && !middleName.isEmpty()) {
                fullName.append(" ").append(middleName);
            }
            fullName.append(" ").append(lastName);
            if (suffix != null && !suffix.isEmpty()) {
                fullName.append(" ").append(suffix);
            }
            return fullName.toString();
        }
    }
}