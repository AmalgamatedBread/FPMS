package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import com.fpms.fpms_backend.repository.DepartmentRepository;
import com.fpms.fpms_backend.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;

    public ResponseEntity<?> getAllDepartments() {
        try {
            List<Department> departments = departmentRepository.findAll();
            List<Map<String, Object>> departmentList = departments.stream()
                    .map(dept -> {
                        Map<String, Object> deptMap = new HashMap<>();
                        deptMap.put("deptCode", dept.getDeptCode());
                        deptMap.put("deptName", dept.getDeptName());
                        deptMap.put("officeLocation", dept.getOfficeLocation());
                        deptMap.put("description", dept.getDescription());
                        deptMap.put("chairperson", dept.getChairperson() != null ?
                                Map.of("id", dept.getChairperson().getFacultyId(),
                                        "name", dept.getChairperson().getFirstName() + " " + dept.getChairperson().getLastName()) : null);
                        deptMap.put("createdAt", dept.getCreatedAt());
                        return deptMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "departments", departmentList
            ));
        } catch (Exception e) {
            log.error("Failed to fetch departments: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch departments: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> getDepartmentByCode(String deptCode) {
        try {
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("deptCode", department.getDeptCode());
            deptMap.put("deptName", department.getDeptName());
            deptMap.put("officeLocation", department.getOfficeLocation());
            deptMap.put("description", department.getDescription());
            deptMap.put("chairperson", department.getChairperson() != null ?
                    Map.of("id", department.getChairperson().getFacultyId(),
                            "name", department.getChairperson().getFirstName() + " " + department.getChairperson().getLastName()) : null);
            deptMap.put("createdAt", department.getCreatedAt());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "department", deptMap
            ));
        } catch (Exception e) {
            log.error("Failed to fetch department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch department: " + e.getMessage()));
        }
    }

    public ResponseEntity<?> createDepartment(Map<String, Object> departmentData) {
        try {
            Department department = Department.builder()
                    .deptCode((String) departmentData.get("deptCode"))
                    .deptName((String) departmentData.get("deptName"))
                    .officeLocation((String) departmentData.get("officeLocation"))
                    .description((String) departmentData.get("description"))
                    .createdAt(LocalDateTime.now())
                    .build();

            departmentRepository.save(department);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Department created successfully",
                    "departmentCode", department.getDeptCode()
            ));
        } catch (Exception e) {
            log.error("Failed to create department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create department: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    @Transactional
    public ResponseEntity<?> updateDepartment(String deptCode, Map<String, Object> departmentData) {
        try {
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            if (departmentData.containsKey("deptName")) {
                department.setDeptName((String) departmentData.get("deptName"));
            }
            if (departmentData.containsKey("officeLocation")) {
                department.setOfficeLocation((String) departmentData.get("officeLocation"));
            }
            if (departmentData.containsKey("description")) {
                department.setDescription((String) departmentData.get("description"));
            }

            departmentRepository.save(department);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Department updated successfully",
                    "departmentCode", department.getDeptCode()
            ));
        } catch (Exception e) {
            log.error("Failed to update department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update department: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    @Transactional
    public ResponseEntity<?> deleteDepartment(String deptCode) {
        try {
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Check if department has faculty members
            List<Faculty> facultyMembers = facultyRepository.findByDepartmentDeptCode(deptCode);
            if (!facultyMembers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cannot delete department. It still has faculty members assigned."
                ));
            }

            departmentRepository.delete(department);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Department deleted successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to delete department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete department: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    public ResponseEntity<?> getDepartmentFaculty(String deptCode) {
        try {
            // Check if department exists
            departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            List<Faculty> facultyList = facultyRepository.findByDepartmentDeptCode(deptCode);
            List<Map<String, Object>> facultyData = facultyList.stream()
                    .map(faculty -> {
                        Map<String, Object> facultyMap = new HashMap<>();
                        facultyMap.put("id", faculty.getFacultyId());
                        facultyMap.put("firstName", faculty.getFirstName());
                        facultyMap.put("middleName", faculty.getMiddleName());
                        facultyMap.put("lastName", faculty.getLastName());
                        facultyMap.put("suffix", faculty.getSuffix());
                        facultyMap.put("email", faculty.getEmail());
                        facultyMap.put("contactNo", faculty.getTelNo());
                        facultyMap.put("role", faculty.getRole().name());
                        return facultyMap;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "faculty", facultyData,
                    "count", facultyData.size()
            ));
        } catch (Exception e) {
            log.error("Failed to fetch department faculty: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch department faculty: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    @Transactional
    public ResponseEntity<?> assignChairperson(String deptCode, Long facultyId) {
        try {
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Check if faculty belongs to this department
            if (faculty.getDepartment() == null || !faculty.getDepartment().getDeptCode().equals(deptCode)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Faculty is not a member of this department"
                ));
            }

            department.setChairperson(faculty);
            departmentRepository.save(department);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Chairperson assigned successfully",
                    "chairperson", Map.of(
                            "id", faculty.getFacultyId(),
                            "name", faculty.getFirstName() + " " + faculty.getLastName()
                    )
            ));
        } catch (Exception e) {
            log.error("Failed to assign chairperson: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to assign chairperson: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    public ResponseEntity<?> getDepartmentByFaculty(Long facultyId) {
        try {
            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));

            if (faculty.getDepartment() == null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Faculty is not assigned to any department",
                        "department", null
                ));
            }

            Department department = faculty.getDepartment();
            Map<String, Object> deptMap = new HashMap<>();
            deptMap.put("deptCode", department.getDeptCode());
            deptMap.put("deptName", department.getDeptName());
            deptMap.put("officeLocation", department.getOfficeLocation());
            deptMap.put("description", department.getDescription());
            deptMap.put("chairperson", department.getChairperson() != null ?
                    Map.of("id", department.getChairperson().getFacultyId(),
                            "name", department.getChairperson().getFirstName() + " " + department.getChairperson().getLastName()) : null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "department", deptMap
            ));
        } catch (Exception e) {
            log.error("Failed to fetch faculty department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch faculty department: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    public ResponseEntity<?> getDepartmentStats(String deptCode) {
        try {
            // Check if department exists
            departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            List<Faculty> facultyList = facultyRepository.findByDepartmentDeptCode(deptCode);

            long totalFaculty = facultyList.size();
            long facultyCount = facultyList.stream()
                    .filter(f -> f.getRole().name().equals("FACULTY"))
                    .count();
            long headCount = facultyList.stream()
                    .filter(f -> f.getRole().name().equals("DEPT_HEAD"))
                    .count();

            // Get chairperson
            Department department = departmentRepository.findById(deptCode).get();
            boolean hasChairperson = department.getChairperson() != null;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalFaculty", totalFaculty);
            stats.put("regularFaculty", facultyCount);
            stats.put("departmentHeads", headCount);
            stats.put("hasChairperson", hasChairperson);
            if (hasChairperson) {
                stats.put("chairpersonName",
                        department.getChairperson().getFirstName() + " " + department.getChairperson().getLastName());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stats", stats
            ));
        } catch (Exception e) {
            log.error("Failed to fetch department stats: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch department stats: " + e.getMessage()));
        }
    }

    // ADDED: Missing method called by DepartmentController
    @Transactional
    public ResponseEntity<?> addFacultyToDepartment(String deptCode, Long facultyId) {
        try {
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new RuntimeException("Faculty not found"));

            // Check if faculty is already in a department
            if (faculty.getDepartment() != null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Faculty is already assigned to department: " + faculty.getDepartment().getDeptCode()
                ));
            }

            faculty.setDepartment(department);
            facultyRepository.save(faculty);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Faculty added to department successfully",
                    "faculty", Map.of(
                            "id", faculty.getFacultyId(),
                            "name", faculty.getFirstName() + " " + faculty.getLastName()
                    ),
                    "department", department.getDeptName()
            ));
        } catch (Exception e) {
            log.error("Failed to add faculty to department: ", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add faculty to department: " + e.getMessage()));
        }
    }

    // ADD: New method for comprehensive department data
    public Map<String, Object> getComprehensiveDepartmentData(String deptCode) {
        try {
            // Get department
            Department department = departmentRepository.findById(deptCode)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            // Get all faculty in department
            List<Faculty> facultyList = facultyRepository.findByDepartmentDeptCode(deptCode);

            // Get leadership (Dean and Dept Head)
            Optional<Faculty> dean = facultyList.stream()
                    .filter(f -> f.getRole().equals(FacultyRole.DEAN))
                    .findFirst();

            Optional<Faculty> deptHead = facultyList.stream()
                    .filter(f -> f.getRole().equals(FacultyRole.DEPT_HEAD))
                    .findFirst();

            // Get chairperson
            Faculty chairperson = department.getChairperson();

            // Prepare department data
            Map<String, Object> departmentData = new HashMap<>();
            departmentData.put("deptCode", department.getDeptCode());
            departmentData.put("deptName", department.getDeptName());
            departmentData.put("officeLocation", department.getOfficeLocation());
            departmentData.put("description", department.getDescription());
            departmentData.put("createdAt", department.getCreatedAt());

            // Prepare leadership data
            Map<String, Object> leadershipData = new HashMap<>();

            // Dean data
            if (dean.isPresent()) {
                Faculty deanFaculty = dean.get();
                leadershipData.put("dean", createFacultyMap(deanFaculty));
            }

            // Department Head data
            if (deptHead.isPresent()) {
                Faculty headFaculty = deptHead.get();
                leadershipData.put("departmentHead", createFacultyMap(headFaculty));
            }

            // Chairperson data
            if (chairperson != null) {
                leadershipData.put("chairperson", createFacultyMap(chairperson));
            }

            // Prepare faculty members data
            List<Map<String, Object>> facultyData = facultyList.stream()
                    .map(this::createFacultyMap)
                    .collect(Collectors.toList());

            // Prepare stats
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("totalFaculty", facultyList.size());
            statsData.put("deanCount", facultyList.stream()
                    .filter(f -> f.getRole().equals(FacultyRole.DEAN))
                    .count());
            statsData.put("deptHeadCount", facultyList.stream()
                    .filter(f -> f.getRole().equals(FacultyRole.DEPT_HEAD))
                    .count());
            statsData.put("regularFacultyCount", facultyList.stream()
                    .filter(f -> f.getRole().equals(FacultyRole.FACULTY))
                    .count());
            statsData.put("hasChairperson", chairperson != null);

            // Return comprehensive data
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("department", departmentData);
            result.put("leadership", leadershipData);
            result.put("facultyMembers", facultyData);
            result.put("stats", statsData);

            return result;

        } catch (Exception e) {
            log.error("Failed to fetch comprehensive department data: ", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        }
    }

    // Helper method to create faculty map
    private Map<String, Object> createFacultyMap(Faculty faculty) {
        Map<String, Object> facultyMap = new HashMap<>();
        facultyMap.put("id", faculty.getFacultyId());
        facultyMap.put("firstName", faculty.getFirstName());
        facultyMap.put("middleName", faculty.getMiddleName());
        facultyMap.put("lastName", faculty.getLastName());
        facultyMap.put("suffix", faculty.getSuffix());
        facultyMap.put("fullName", getFullName(faculty));
        facultyMap.put("email", faculty.getEmail());
        facultyMap.put("telNo", faculty.getTelNo());
        facultyMap.put("role", faculty.getRole().name());
        facultyMap.put("address", faculty.getAddress());
        facultyMap.put("avatarInitial", getAvatarInitial(faculty));
        facultyMap.put("avatarColor", getAvatarColor(faculty.getFirstName() + faculty.getLastName()));
        return facultyMap;
    }

    // Helper method to get full name with suffix
    private String getFullName(Faculty faculty) {
        StringBuilder fullName = new StringBuilder();

        // Add title based on role
        if (faculty.getRole().equals(FacultyRole.DEAN) ||
                faculty.getRole().equals(FacultyRole.DEPT_HEAD)) {
            fullName.append("Dr. ");
        } else {
            fullName.append("Prof. ");
        }

        fullName.append(faculty.getFirstName());

        if (faculty.getMiddleName() != null && !faculty.getMiddleName().isEmpty()) {
            fullName.append(" ").append(faculty.getMiddleName());
        }

        fullName.append(" ").append(faculty.getLastName());

        if (faculty.getSuffix() != null && !faculty.getSuffix().isEmpty()) {
            fullName.append(" ").append(faculty.getSuffix());
        }

        return fullName.toString();
    }

    // Helper method to get avatar initial
    private String getAvatarInitial(Faculty faculty) {
        if (faculty.getFirstName() != null && !faculty.getFirstName().isEmpty()) {
            return faculty.getFirstName().substring(0, 1).toUpperCase();
        }
        return "U";
    }

    // Helper method to get avatar color based on name (consistent color for same person)
    private String getAvatarColor(String name) {
        if (name == null || name.isEmpty()) {
            return "#4f46e5";
        }

        // Simple hash function for consistent color
        int hash = 0;
        for (int i = 0; i < name.length(); i++) {
            hash = name.charAt(i) + ((hash << 5) - hash);
        }

        // Predefined pleasant colors
        String[] colors = {
                "#4f46e5", "#10b981", "#f59e0b", "#ef4444",
                "#8b5cf6", "#06b6d4", "#84cc16", "#f97316"
        };

        int index = Math.abs(hash) % colors.length;
        return colors[index];
    }
}
