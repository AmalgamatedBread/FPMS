package com.fpms.fpms_backend.controller;

import com.fpms.fpms_backend.dto.LoginRequest;
import com.fpms.fpms_backend.dto.RegisterRequest;
import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.Department;
import com.fpms.fpms_backend.model.entities.SystemCredentials;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import com.fpms.fpms_backend.service.AuthService;
import com.fpms.fpms_backend.service.DepartmentService;
import com.fpms.fpms_backend.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final AuthService authService;
    private final SystemCredentialsRepository systemCredentialsRepository;
    private final UserContextService userContextService;
    private final DepartmentService departmentService;
    private final FacultyRepository facultyRepository;

    @GetMapping("/")
    public String homePage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            log.info("Home: User already authenticated, redirecting to dashboard");
            return "redirect:/dashboard";
        }
        log.info("Home: No authentication, redirecting to login");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            log.info("Login page: User already authenticated, redirecting to dashboard");
            return "redirect:/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        model.addAttribute("pageTitle", "Login");
        log.info("Login page: Showing login form");
        return "authentication/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("pageTitle", "Register");
        log.info("Register page: Showing registration form");
        return "authentication/registration";
    }

    @PostMapping("/register")
    public String processRegister(@Valid RegisterRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            log.info("Processing registration form for: {}", request.getEmail());
            return authService.processRegister(request);
        } catch (Exception e) {
            log.error("Registration processing error: ", e);
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboardPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("Dashboard: No authentication, redirecting to login");
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());
        log.info("Dashboard: User {} with role {} - redirecting to role-specific dashboard",
                auth.getName(), role);

        // Add model attributes before redirecting
        model.addAttribute("userRole", role);
        model.addAttribute("username", auth.getName());

        switch (role) {
            case "DEAN":
                return "redirect:/dean-dashboard";
            case "DEPT_HEAD":
                return "redirect:/head-dashboard";
            case "FACULTY":
                return "redirect:/faculty-dashboard";
            default:
                log.warn("Unknown role: {}, redirecting to faculty dashboard", role);
                return "redirect:/faculty-dashboard";
        }
    }
    private String extractRoleFromAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            log.warn("WARNING: No authorities provided");
            return "FACULTY";
        }

        log.info("Extracting role from {} authorities", authorities.size());

        for (GrantedAuthority authority : authorities) {
            String authString = authority.getAuthority();
            log.info("Processing authority: '{}'", authString);

            // Handle ROLE_DEAN, ROLE_DEPT_HEAD, ROLE_FACULTY
            if ("ROLE_DEAN".equals(authString)) {
                log.info("Found ROLE_DEAN, returning DEAN");
                return "DEAN";
            } else if ("ROLE_DEPT_HEAD".equals(authString)) {
                log.info("Found ROLE_DEPT_HEAD, returning DEPT_HEAD");
                return "DEPT_HEAD";
            } else if ("ROLE_FACULTY".equals(authString)) {
                log.info("Found ROLE_FACULTY, returning FACULTY");
                return "FACULTY";
            }

            // Also check without ROLE_ prefix
            if ("DEAN".equals(authString)) {
                log.info("Found DEAN (no prefix), returning DEAN");
                return "DEAN";
            } else if ("DEPT_HEAD".equals(authString)) {
                log.info("Found DEPT_HEAD (no prefix), returning DEPT_HEAD");
                return "DEPT_HEAD";
            } else if ("FACULTY".equals(authString)) {
                log.info("Found FACULTY (no prefix), returning FACULTY");
                return "FACULTY";
            }
        }

        log.warn("WARNING: No known role found in authorities: {}", authorities);
        return "FACULTY";
    }

    @GetMapping("/faculty-dashboard")
    public String facultyDashboard(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("Faculty dashboard: No authentication, redirecting to login");
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        if (!"FACULTY".equals(role)) {
            log.error("Faculty dashboard: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        model.addAttribute("pageTitle", "Faculty Dashboard");
        log.info("Faculty dashboard: Showing dashboard for {}", auth.getName());
        return "faculty/faculty_dashboard";
    }

    @GetMapping("/head-dashboard")
    public String headDashboard(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("Head dashboard: No authentication, redirecting to login");
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        if (!"DEPT_HEAD".equals(role)) {
            log.error("Head dashboard: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        model.addAttribute("pageTitle", "Head Dashboard");
        log.info("Head dashboard: Showing dashboard for {}", auth.getName());
        return "head/head_dashboard";
    }

    @GetMapping("/dean-dashboard")
    public String deanDashboard(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("Dean dashboard: No authentication, redirecting to login");
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        if (!"DEAN".equals(role)) {
            log.error("Dean dashboard: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        model.addAttribute("pageTitle", "Dean Dashboard");
        log.info("Dean dashboard: Showing dashboard for {}", auth.getName());
        return "dean/dean_dashboard";
    }


    @GetMapping("/approval")
    public String approvalPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        model.addAttribute("pageTitle", "Approval Queue");
        model.addAttribute("userRole", role);
        model.addAttribute("username", auth.getName());  // Add this
        return "head/approval";
    }


    @GetMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        model.addAttribute("pageTitle", "Profile");
        return "common/profile";
    }

    @GetMapping("/department")
    public String departmentPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        if (!"FACULTY".equals(role) && !"DEPT_HEAD".equals(role) && !"DEAN".equals(role)) {
            log.error("Department page: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        try {
            Optional<Faculty> currentFacultyOpt = userContextService.getCurrentFaculty();
            if (currentFacultyOpt.isEmpty()) {
                return "redirect:/login";
            }

            Faculty currentFaculty = currentFacultyOpt.get();

            // Check if faculty has a department
            if (currentFaculty.getDepartment() == null) {
                model.addAttribute("hasDepartment", false);
                model.addAttribute("canManage", "DEPT_HEAD".equals(role) || "DEAN".equals(role));
                model.addAttribute("userRole", role);
                model.addAttribute("username", auth.getName());
                model.addAttribute("pageTitle", "Department");
                return "common/department";
            }

            Department department = currentFaculty.getDepartment();
            String deptCode = department.getDeptCode();

            log.info("Loading department {} for user {}", deptCode, auth.getName());

            // Get faculty in this department
            List<Faculty> facultyList = facultyRepository.findByDepartmentDeptCode(deptCode);

            log.info("Department: {} - {}", department.getDeptCode(), department.getDeptName());
            log.info("Department chairperson: {}", department.getChairperson());
            log.info("Total faculty in department: {}", facultyList.size());

            // Find DEAN in this department
            Optional<Faculty> dean = facultyList.stream()
                    .filter(f -> f.getRole() == FacultyRole.DEAN)
                    .findFirst();

            // Find DEPARTMENT HEAD in this department
            Optional<Faculty> departmentHead = facultyList.stream()
                    .filter(f -> f.getRole() == FacultyRole.DEPT_HEAD)
                    .findFirst();

            // Prepare department data
            Map<String, Object> departmentData = new HashMap<>();
            departmentData.put("deptCode", department.getDeptCode());
            departmentData.put("deptName", department.getDeptName());
            departmentData.put("officeLocation", department.getOfficeLocation());
            departmentData.put("description", department.getDescription());
            departmentData.put("createdAt", department.getCreatedAt());

            if (department.getChairperson() != null) {
                Map<String, Object> chairpersonData = new HashMap<>();
                chairpersonData.put("firstName", department.getChairperson().getFirstName());
                chairpersonData.put("lastName", department.getChairperson().getLastName());
                chairpersonData.put("fullName", department.getChairperson().getFirstName() + " " + department.getChairperson().getLastName());
                chairpersonData.put("email", department.getChairperson().getEmail());
                chairpersonData.put("telNo", department.getChairperson().getTelNo());
                chairpersonData.put("avatarInitial", department.getChairperson().getFirstName() != null && !department.getChairperson().getFirstName().isEmpty()
                        ? department.getChairperson().getFirstName().substring(0, 1).toUpperCase() : "C");
                chairpersonData.put("avatarColor", "#10b981");
                departmentData.put("chairperson", chairpersonData);
                departmentData.put("chairpersonName",
                        department.getChairperson().getFirstName() + " " + department.getChairperson().getLastName());
            }

            // Prepare leadership data - IMPORTANT: Always add all keys, even if null
            Map<String, Object> leadershipData = new HashMap<>();

            // Add dean
            if (dean.isPresent()) {
                Faculty deanFaculty = dean.get();
                Map<String, Object> deanMap = new HashMap<>();
                deanMap.put("firstName", deanFaculty.getFirstName());
                deanMap.put("lastName", deanFaculty.getLastName());
                deanMap.put("fullName", deanFaculty.getFirstName() + " " + deanFaculty.getLastName());
                deanMap.put("email", deanFaculty.getEmail());
                deanMap.put("telNo", deanFaculty.getTelNo());
                deanMap.put("avatarInitial", deanFaculty.getFirstName() != null && !deanFaculty.getFirstName().isEmpty()
                        ? deanFaculty.getFirstName().substring(0, 1).toUpperCase() : "D");
                deanMap.put("avatarColor", "#4f46e5");
                leadershipData.put("dean", deanMap);
            } else {
                leadershipData.put("dean", null); // Explicitly add null
            }

            // Add departmentHead
            if (departmentHead.isPresent()) {
                Faculty headFaculty = departmentHead.get();
                Map<String, Object> headMap = new HashMap<>();
                headMap.put("firstName", headFaculty.getFirstName());
                headMap.put("lastName", headFaculty.getLastName());
                headMap.put("fullName", headFaculty.getFirstName() + " " + headFaculty.getLastName());
                headMap.put("email", headFaculty.getEmail());
                headMap.put("telNo", headFaculty.getTelNo());
                headMap.put("avatarInitial", headFaculty.getFirstName() != null && !headFaculty.getFirstName().isEmpty()
                        ? headFaculty.getFirstName().substring(0, 1).toUpperCase() : "H");
                headMap.put("avatarColor", "#059669");
                leadershipData.put("departmentHead", headMap);
            } else {
                leadershipData.put("departmentHead", null); // Explicitly add null
            }

            // Add chairperson (from department entity)
            if (department.getChairperson() != null) {
                Faculty chairperson = department.getChairperson();
                Map<String, Object> chairpersonMap = new HashMap<>();
                chairpersonMap.put("firstName", chairperson.getFirstName());
                chairpersonMap.put("lastName", chairperson.getLastName());
                chairpersonMap.put("fullName", chairperson.getFirstName() + " " + chairperson.getLastName());
                chairpersonMap.put("email", chairperson.getEmail());
                chairpersonMap.put("telNo", chairperson.getTelNo());
                chairpersonMap.put("avatarInitial", chairperson.getFirstName() != null && !chairperson.getFirstName().isEmpty()
                        ? chairperson.getFirstName().substring(0, 1).toUpperCase() : "C");
                chairpersonMap.put("avatarColor", "#10b981");
                leadershipData.put("chairperson", chairpersonMap);
            } else {
                leadershipData.put("chairperson", null); // Explicitly add null
            }

            // Prepare faculty members data
            List<Map<String, Object>> facultyData = facultyList.stream()
                    .map(faculty -> {
                        Map<String, Object> facultyMap = new HashMap<>();
                        facultyMap.put("id", faculty.getFacultyId());
                        facultyMap.put("firstName", faculty.getFirstName());
                        facultyMap.put("lastName", faculty.getLastName());
                        facultyMap.put("fullName", faculty.getFirstName() + " " + faculty.getLastName());
                        facultyMap.put("email", faculty.getEmail());
                        facultyMap.put("telNo", faculty.getTelNo());
                        facultyMap.put("role", faculty.getRole().name());
                        facultyMap.put("avatarInitial", faculty.getFirstName() != null && !faculty.getFirstName().isEmpty()
                                ? faculty.getFirstName().substring(0, 1).toUpperCase() : "U");
                        facultyMap.put("avatarColor", "#4f46e5");
                        return facultyMap;
                    })
                    .collect(Collectors.toList());

            // Prepare stats
            Map<String, Object> statsData = new HashMap<>();
            statsData.put("totalFaculty", facultyList.size());
            statsData.put("regularFacultyCount", facultyList.stream()
                    .filter(f -> f.getRole() == FacultyRole.FACULTY)
                    .count());
            statsData.put("deanCount", facultyList.stream()
                    .filter(f -> f.getRole() == FacultyRole.DEAN)
                    .count());
            statsData.put("departmentHeadCount", facultyList.stream()
                    .filter(f -> f.getRole() == FacultyRole.DEPT_HEAD)
                    .count());

            // Add all data to model
            model.addAttribute("department", departmentData);
            model.addAttribute("leadership", leadershipData); // Now has all keys even if null
            model.addAttribute("facultyMembers", facultyData);
            model.addAttribute("stats", statsData);
            model.addAttribute("hasDepartment", true);
            model.addAttribute("canManage", "DEPT_HEAD".equals(role) || "DEAN".equals(role));
            model.addAttribute("userRole", role);
            model.addAttribute("username", auth.getName());
            model.addAttribute("pageTitle", "Department");

            log.info("Department page loaded successfully with {} faculty members", facultyList.size());
            log.info("Leadership data keys: {}", leadershipData.keySet());

            return "common/department";

        } catch (Exception e) {
            log.error("Error loading department page: ", e);
            model.addAttribute("error", "Failed to load department data: " + e.getMessage());
            model.addAttribute("hasDepartment", false);
            model.addAttribute("canManage", false);
            model.addAttribute("userRole", role);
            model.addAttribute("username", auth.getName());
            model.addAttribute("pageTitle", "Department");
            return "common/department";
        }
    }

    @GetMapping("/assign-department")
    public String assignDepartmentPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        // Check if user already has department
        Optional<Faculty> faculty = userContextService.getCurrentFaculty();
        if (faculty.isPresent() && faculty.get().getDepartment() != null) {
            return "redirect:/department";
        }

        return "common/assign-department";
    }

    @GetMapping("/bin")
    public String binPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "Bin");
        return "common/bin";
    }

    @GetMapping("/forward-docs")
    public String forwardDocsPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());
        if (!"DEPT_HEAD".equals(role) && !"DEAN".equals(role) && !"FACULTY".equals(role)) {
            log.error("Forward docs page: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        model.addAttribute("pageTitle", "Forward Documents");
        return "common/forward_docs";
    }

    // FIXED: Redirect faculty to main portfolio system
    @GetMapping("/my-portfolio")
    public String myPortfolioPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String role = extractRoleFromAuthorities(auth.getAuthorities());
        if (!"FACULTY".equals(role)) {
            return "redirect:/access-denied";
        }
        // UNIFIED: All roles go to the same portfolio page
        return "redirect:/portfolio";
    }

    @GetMapping("/submissions")
    public String submissionsPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String role = extractRoleFromAuthorities(auth.getAuthorities());
        if (!"FACULTY".equals(role)) {
            return "redirect:/access-denied";
        }
        model.addAttribute("pageTitle", "My Submissions");
        return "faculty/submissions";
    }

    @GetMapping("/documents")
    public String documentsPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String role = extractRoleFromAuthorities(auth.getAuthorities());
        if (!"FACULTY".equals(role)) {
            return "redirect:/access-denied";
        }
        model.addAttribute("pageTitle", "Documents");
        return "faculty/documents";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            log.info("User logged out successfully");
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/access-denied")
    public String accessDeniedPage(Model model) {
        log.warn("Access denied page accessed");
        model.addAttribute("pageTitle", "Access Denied");
        return "common/access-denied";
    }

    @GetMapping("/settings")
    public String settingsPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "Settings");
        return "common/settings";
    }

    @GetMapping("/notifications")
    public String notificationsPage(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        model.addAttribute("pageTitle", "Notifications");
        return "common/notifications";
    }

    // FIXED: Allow ALL roles to access portfolios
    @GetMapping("/portfolios")
    public String portfoliosPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String role = extractRoleFromAuthorities(auth.getAuthorities());

        // UNIFIED: ALL ROLES can access portfolios
        if (!"DEPT_HEAD".equals(role) && !"DEAN".equals(role) && !"FACULTY".equals(role)) {
            log.error("Portfolios page: Access denied for role {}", role);
            return "redirect:/access-denied";
        }

        model.addAttribute("pageTitle", "Portfolios");
        model.addAttribute("userRole", role);
        model.addAttribute("username", auth.getName());

        // Redirect to the unified PortfolioController
        return "redirect:/portfolio";
    }
}