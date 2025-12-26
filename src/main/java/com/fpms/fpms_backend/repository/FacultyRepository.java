package com.fpms.fpms_backend.repository;

import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    // Check if email exists
    boolean existsByEmail(String email);

    // Find all faculty in a specific department
    List<Faculty> findByDepartmentDeptCode(String deptCode);

    // Find faculty in a department with specific role
    List<Faculty> findByDepartmentDeptCodeAndRole(String deptCode, FacultyRole role);

    // Find faculty without department OR not in specified department
    @Query("SELECT f FROM Faculty f WHERE f.department IS NULL OR f.department.deptCode != :deptCode")
    List<Faculty> findByDepartmentIsNullOrDepartmentDeptCodeNot(@Param("deptCode") String deptCode);

    // Find faculty without any department
    List<Faculty> findByDepartmentIsNull();

    // Find faculty available for head assignment (not already heads and in the department)
    @Query("SELECT f FROM Faculty f WHERE f.department.deptCode = :deptCode " +
            "AND f.role != 'DEPT_HEAD' AND f.role != 'DEAN'")
    List<Faculty> findAvailableForHeadAssignment(@Param("deptCode") String deptCode);

    // Find faculty by ID with department
    @Query("SELECT f FROM Faculty f LEFT JOIN FETCH f.department WHERE f.facultyId = :facultyId")
    Optional<Faculty> findByIdWithDepartment(@Param("facultyId") Long facultyId);

    // Find department head for a department
    @Query("SELECT f FROM Faculty f WHERE f.department.deptCode = :deptCode AND f.role = 'DEPT_HEAD'")
    Optional<Faculty> findDepartmentHead(@Param("deptCode") String deptCode);

    @Query("SELECT f FROM Faculty f WHERE f.department = :department AND f.role != 'FACULTY'")
    List<Faculty> findDepartmentLeaders(@Param("department") Department department);

    @Query("SELECT f FROM Faculty f WHERE f.email = :email")
    Optional<Faculty> findUserByEmail(@Param("email") String email);

    // Add this @Query annotation instead of findByUsername
    @Query("SELECT f FROM Faculty f JOIN SystemCredentials sc ON f.facultyId = sc.faculty.facultyId WHERE sc.username = :username")
    Optional<Faculty> findBySystemCredentialsUsername(@Param("username") String username);


    Optional<Faculty> findByEmail(String email);

    Optional<Faculty> findByFirstNameAndLastName(String firstName, String lastName);

    List<Faculty> findByRole(FacultyRole role);

    List<Faculty> findByDepartmentAndRole(Department department, FacultyRole role);

    //Optional<Faculty> findByUsername(String username);

}