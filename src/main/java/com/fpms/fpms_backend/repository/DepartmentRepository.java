package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    Optional<Department> findByDeptCode(String deptCode);
    boolean existsByDeptCode(String deptCode);
    boolean existsByDeptName(String deptName);
}