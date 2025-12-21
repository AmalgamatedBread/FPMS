package com.fpms.fpms_backend.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "department")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @Column(name = "dept_code", length = 10)
    private String deptCode;

    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;

    @Column(name = "office_location", length = 100)
    private String officeLocation;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chairperson_id", referencedColumnName = "faculty_id")
    private Faculty chairperson;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Faculty> facultyMembers;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}