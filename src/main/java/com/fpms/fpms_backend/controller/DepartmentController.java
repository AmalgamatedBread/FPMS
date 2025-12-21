package com.fpms.fpms_backend.controller;

import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.repository.DepartmentRepository;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.service.DepartmentService;
import com.fpms.fpms_backend.service.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;
    private final UserContextService userContextService;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;

    // Helper method to get current faculty with proper error handling
    private Faculty getCurrentFaculty() {
        return userContextService.getCurrentFaculty()
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }

    @GetMapping
    public ResponseEntity<?> getAllDepartments() {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Fetching all departments for user: {}", currentFaculty.getEmail());
            return departmentService.getAllDepartments();
        } catch (Exception e) {
            log.error("Failed to fetch departments: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{deptCode}")
    public ResponseEntity<?> getDepartment(@PathVariable String deptCode) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Fetching department {} for user: {}", deptCode, currentFaculty.getEmail());
            return departmentService.getDepartmentByCode(deptCode);
        } catch (Exception e) {
            log.error("Failed to fetch department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createDepartment(@RequestBody Map<String, Object> departmentData) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Creating department by user: {}", currentFaculty.getEmail());
            return departmentService.createDepartment(departmentData);
        } catch (Exception e) {
            log.error("Failed to create department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{deptCode}")
    public ResponseEntity<?> updateDepartment(@PathVariable String deptCode,
                                              @RequestBody Map<String, Object> departmentData) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Updating department {} by user: {}", deptCode, currentFaculty.getEmail());
            return departmentService.updateDepartment(deptCode, departmentData);
        } catch (Exception e) {
            log.error("Failed to update department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{deptCode}")
    public ResponseEntity<?> deleteDepartment(@PathVariable String deptCode) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Deleting department {} by user: {}", deptCode, currentFaculty.getEmail());
            return departmentService.deleteDepartment(deptCode);
        } catch (Exception e) {
            log.error("Failed to delete department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{deptCode}/faculty")
    public ResponseEntity<?> getDepartmentFaculty(@PathVariable String deptCode) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Fetching faculty for department {} by user: {}", deptCode, currentFaculty.getEmail());
            return departmentService.getDepartmentFaculty(deptCode);
        } catch (Exception e) {
            log.error("Failed to fetch department faculty: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{deptCode}/assign-chairperson")
    public ResponseEntity<?> assignChairperson(@PathVariable String deptCode,
                                               @RequestBody Map<String, Long> request) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            Long facultyId = request.get("facultyId");
            log.info("Assigning chairperson {} to department {} by user: {}",
                    facultyId, deptCode, currentFaculty.getEmail());
            return departmentService.assignChairperson(deptCode, facultyId);
        } catch (Exception e) {
            log.error("Failed to assign chairperson: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-department")
    public ResponseEntity<?> getMyDepartment() {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Fetching department for user: {}", currentFaculty.getEmail());
            return departmentService.getDepartmentByFaculty(currentFaculty.getFacultyId());
        } catch (Exception e) {
            log.error("Failed to fetch user's department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{deptCode}/stats")
    public ResponseEntity<?> getDepartmentStats(@PathVariable String deptCode) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            log.info("Fetching stats for department {} by user: {}", deptCode, currentFaculty.getEmail());
            return departmentService.getDepartmentStats(deptCode);
        } catch (Exception e) {
            log.error("Failed to fetch department stats: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{deptCode}/add-faculty")
    public ResponseEntity<?> addFacultyToDepartment(@PathVariable String deptCode,
                                                    @RequestBody Map<String, Long> request) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            Long facultyId = request.get("facultyId");
            log.info("Adding faculty {} to department {} by user: {}",
                    facultyId, deptCode, currentFaculty.getEmail());
            return departmentService.addFacultyToDepartment(deptCode, facultyId);
        } catch (Exception e) {
            log.error("Failed to add faculty to department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Add to DepartmentController.java
    @PostMapping("/assign-to-department")
    public ResponseEntity<?> assignFacultyToDepartment(@RequestBody Map<String, Object> request) {
        try {
            Faculty currentFaculty = getCurrentFaculty();
            String deptCode = (String) request.get("deptCode");

            log.info("Assigning faculty {} to department {} by user: {}",
                    currentFaculty.getFacultyId(), deptCode, currentFaculty.getEmail());

            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            currentFaculty.setDepartment(department);
            facultyRepository.save(currentFaculty);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully assigned to department: " + department.getDeptName(),
                    "department", Map.of(
                            "code", department.getDeptCode(),
                            "name", department.getDeptName()
                    )
            ));
        } catch (Exception e) {
            log.error("Failed to assign department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}