package com.fpms.fpms_backend.model.entities;

import com.fpms.fpms_backend.model.enums.FacultyRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "faculty")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "tel_no")
    private String telNo;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private FacultyRole role;

    @ManyToOne
    @JoinColumn(name = "dept_code")
    private Department department;

    @OneToOne(mappedBy = "faculty", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SystemCredentials systemCredentials;

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