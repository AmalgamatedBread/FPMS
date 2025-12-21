package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.SystemCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SystemCredentialsRepository extends JpaRepository<SystemCredentials, Long> {
    Optional<SystemCredentials> findByUsername(String username);

    // Fixed with explicit query
    @Query("SELECT sc FROM SystemCredentials sc WHERE sc.faculty.facultyId = :facultyId")
    Optional<SystemCredentials> findByFacultyId(@Param("facultyId") Long facultyId);

    Optional<SystemCredentials> findByFaculty_FacultyId(Long facultyId);

    boolean existsByUsername(String username);
}