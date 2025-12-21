package com.fpms.fpms_backend.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "account_type")
    private String accountType;

    // CHANGE FROM LAZY TO EAGER
    @OneToOne(fetch = FetchType.EAGER) // CHANGED FROM LAZY TO EAGER
    @JoinColumn(name = "faculty_id", referencedColumnName = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}