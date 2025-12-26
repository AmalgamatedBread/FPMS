package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.model.entities.*;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import com.fpms.fpms_backend.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioShareRepository portfolioShareRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository systemCredentialsRepository;
    private final EntityManager entityManager;

    private final String UPLOAD_BASE_DIR = System.getProperty("user.dir") + "/uploads/portfolios/";

    private Faculty getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.error("User not authenticated");
            throw new IllegalStateException("User not authenticated");
        }

        String username = auth.getName();
        log.info("Getting current user with username: {}", username);

        // Try multiple approaches
        Faculty user = null;

        Optional<SystemCredentials> credentials = systemCredentialsRepository.findByUsername(username);
        if (credentials.isPresent() && credentials.get().getFaculty() != null) {
            user = credentials.get().getFaculty();
            log.info("Found user via SystemCredentials: {}", user.getEmail());
            return user;
        }

        Optional<Faculty> facultyOpt = facultyRepository.findByEmail(username);
        if (facultyOpt.isPresent()) {
            user = facultyOpt.get();
            log.info("Found user via email: {}", user.getEmail());
            return user;
        }

        log.error("User not found with any method for username: {}", username);
        throw new IllegalStateException("User not found with username/email: " + username);
    }

    public Portfolio getPortfolioById(Long portfolioId) {
        try {
            return portfolioRepository.findById(portfolioId)
                    .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));
        } catch (Exception e) {
            log.error("Error getting portfolio by ID {}: {}", portfolioId, e.getMessage());
            return null;
        }
    }

    @Transactional
    public Portfolio createPortfolio(String name, String description, Portfolio.PortfolioType type) {
        Faculty currentUser = getCurrentUser();
        log.info("Creating portfolio: {} (Type: {}) for user: {}", name, type, currentUser.getEmail());

        if (portfolioRepository.existsByNameAndOwner(name, currentUser)) {
            throw new IllegalArgumentException("You already have a portfolio with this name");
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setDescription(description);
        portfolio.setType(type);
        portfolio.setOwner(currentUser);

        if (type != Portfolio.PortfolioType.PERSONAL) {
            Department department = currentUser.getDepartment();
            if (department == null) {
                throw new IllegalStateException("You must be assigned to a department to create department/college portfolios");
            }
            portfolio.setDepartment(department);
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        log.info("Created portfolio: {} (ID: {}, Type: {}) for user: {}",
                name, savedPortfolio.getId(), type, currentUser.getEmail());

        return savedPortfolio;
    }

    public List<Portfolio> getUserPortfolios() {
        try {
            Faculty currentUser = getCurrentUser();
            log.info("Getting portfolios for user ID: {}, Email: {}",
                    currentUser.getFacultyId(), currentUser.getEmail());

            List<Portfolio> portfolios = portfolioRepository.findByOwner(currentUser);

            if (portfolios.isEmpty()) {
                log.info("No portfolios found via repository method, trying alternative...");
                String sql = "SELECT p.* FROM portfolios p WHERE p.owner_id = ?";
                @SuppressWarnings("unchecked")
                List<Portfolio> sqlPortfolios = entityManager.createNativeQuery(sql, Portfolio.class)
                        .setParameter(1, currentUser.getFacultyId())
                        .getResultList();
                portfolios = sqlPortfolios;
            }

            log.info("Found {} portfolios for user: {}", portfolios.size(), currentUser.getEmail());

            for (Portfolio portfolio : portfolios) {
                log.info("Portfolio: {} (ID: {}) owned by {}",
                        portfolio.getName(),
                        portfolio.getId(),
                        portfolio.getOwner() != null ?
                                portfolio.getOwner().getEmail() : "NULL OWNER");
            }

            return portfolios;

        } catch (Exception e) {
            log.error("Error getting user portfolios: ", e);
            return Collections.emptyList();
        }
    }

    public List<Portfolio> getDepartmentPortfolios() {
        try {
            Faculty currentUser = getCurrentUser();

            log.info("Getting department portfolios for user: {} (Role: {})",
                    currentUser.getEmail(), currentUser.getRole());

            Department department = currentUser.getDepartment();

            if (department == null) {
                log.warn("User {} is not assigned to any department", currentUser.getEmail());
                return Collections.emptyList();
            }

            if (currentUser.getRole() != FacultyRole.DEPT_HEAD && currentUser.getRole() != FacultyRole.DEAN) {
                log.warn("User {} (Role: {}) tried to access department portfolios without permission",
                        currentUser.getEmail(), currentUser.getRole());
                return Collections.emptyList();
            }

            List<Portfolio> portfolios = portfolioRepository.findByDepartmentAndType(department, Portfolio.PortfolioType.DEPARTMENT);
            log.info("Found {} department portfolios for department: {}",
                    portfolios.size(), department.getDeptName());
            return portfolios;

        } catch (Exception e) {
            log.error("Error fetching department portfolios: ", e);
            return Collections.emptyList();
        }
    }

    public List<PortfolioShare> getSharedWithMe() {
        try {
            Faculty currentUser = getCurrentUser();
            List<PortfolioShare> shared = portfolioShareRepository.findBySharedWith(currentUser);
            log.info("Found {} shared portfolios for user: {}",
                    shared.size(), currentUser.getEmail());
            return shared;
        } catch (Exception e) {
            log.error("Error getting shared portfolios: ", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public PortfolioItem uploadFile(MultipartFile file, Long portfolioId, Long folderId, String comments) throws IOException {
        Faculty currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (!hasAccess(portfolio, currentUser)) {
            throw new IllegalStateException("You don't have permission to upload to this portfolio");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        if (contentType == null) {
            throw new IllegalArgumentException("File type not recognized");
        }

        if (contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Video files are not allowed. Please upload only documents and images.");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String userDir = currentUser.getFacultyId().toString();
        String portfolioDir = portfolioId.toString();
        Path uploadPath = Paths.get(UPLOAD_BASE_DIR, userDir, portfolioDir);
        Files.createDirectories(uploadPath);

        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        PortfolioItem item = new PortfolioItem();
        item.setName(originalFilename);
        item.setFilePath(filePath.toString());
        item.setFileType(contentType);
        item.setFileSize(file.getSize());
        item.setFolder(false);
        item.setPortfolio(portfolio);
        item.setUploadedBy(currentUser);
        item.setComments(comments);

        if (folderId != null) {
            PortfolioItem parentFolder = portfolioItemRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
            item.setParentFolder(parentFolder);
        }

        if (currentUser.getRole() == FacultyRole.FACULTY) {
            item.setStatus("PENDING");

            ApprovalRequest request = new ApprovalRequest();
            request.setItem(item);
            request.setSubmittedBy(currentUser);
            request.setComments(comments);
            request.setStatus(ApprovalRequest.ApprovalStatus.PENDING);
            approvalRequestRepository.save(request);

            log.info("Created pending approval request for file: {} uploaded by: {}",
                    originalFilename, currentUser.getEmail());
        } else {
            item.setStatus("APPROVED");
        }

        PortfolioItem savedItem = portfolioItemRepository.save(item);
        log.info("Uploaded file: {} (Size: {} bytes, Type: {}) to portfolio: {}",
                originalFilename, file.getSize(), contentType, portfolio.getName());

        return savedItem;
    }

    @Transactional
    public void deletePortfolio(Long portfolioId) {
        Faculty currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (!portfolio.getOwner().equals(currentUser)) {
            throw new IllegalStateException("You don't own this portfolio");
        }

        List<PortfolioItem> items = portfolioItemRepository.findByPortfolio(portfolio);
        portfolioItemRepository.deleteAll(items);

        List<PortfolioShare> shares = portfolioShareRepository.findByPortfolio(portfolio);
        portfolioShareRepository.deleteAll(shares);

        portfolioRepository.delete(portfolio);
        log.info("Deleted portfolio: {} by user: {}", portfolio.getName(), currentUser.getEmail());
    }

    @Transactional
    public PortfolioShare sharePortfolio(Long portfolioId, Long facultyId, PortfolioShare.SharePermission permission) {
        Faculty currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (!portfolio.getOwner().equals(currentUser)) {
            throw new IllegalStateException("You don't have permission to share this portfolio");
        }

        Faculty shareWith = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

        if (portfolioShareRepository.existsByPortfolioAndSharedWith(portfolio, shareWith)) {
            throw new IllegalArgumentException("Portfolio already shared with this user");
        }

        PortfolioShare share = new PortfolioShare();
        share.setPortfolio(portfolio);
        share.setSharedWith(shareWith);
        share.setPermission(permission);

        PortfolioShare savedShare = portfolioShareRepository.save(share);
        log.info("Shared portfolio: {} with user: {} (Permission: {}) by: {}",
                portfolio.getName(), shareWith.getEmail(), permission, currentUser.getEmail());

        return savedShare;
    }

    public List<PortfolioItem> getPortfolioItems(Long portfolioId, Long folderId) {
        Faculty currentUser = getCurrentUser();
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (!hasAccess(portfolio, currentUser)) {
            throw new IllegalStateException("You don't have permission to view this portfolio");
        }

        List<PortfolioItem> items;
        if (folderId != null) {
            PortfolioItem folder = portfolioItemRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
            items = portfolioItemRepository.findByParentFolder(folder);
        } else {
            items = portfolioItemRepository.findByPortfolioAndParentFolderIsNull(portfolio);
        }

        log.info("Found {} items in portfolio: {} (folderId: {})",
                items.size(), portfolio.getName(), folderId);

        return items;
    }

    private boolean hasAccess(Portfolio portfolio, Faculty user) {
        if (portfolio == null || user == null) {
            return false;
        }

        if (portfolio.getOwner() != null && portfolio.getOwner().getFacultyId().equals(user.getFacultyId())) {
            return true;
        }

        if (portfolioShareRepository.existsByPortfolioAndSharedWith(portfolio, user)) {
            return true;
        }

        if (portfolio.getType() == Portfolio.PortfolioType.DEPARTMENT &&
                portfolio.getDepartment() != null &&
                user.getDepartment() != null &&
                portfolio.getDepartment().getDeptCode().equals(user.getDepartment().getDeptCode()) &&
                (user.getRole() == FacultyRole.DEPT_HEAD || user.getRole() == FacultyRole.DEAN)) {
            return true;
        }

        if (portfolio.getType() == Portfolio.PortfolioType.COLLEGE &&
                user.getRole() == FacultyRole.DEAN) {
            return true;
        }

        return false;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public boolean hasAccessToItem(Faculty user, PortfolioItem item) {
        if (item == null || user == null) {
            return false;
        }

        if (item.getUploadedBy() != null &&
                item.getUploadedBy().getFacultyId().equals(user.getFacultyId())) {
            return true;
        }

        if (item.getPortfolio().getOwner().getFacultyId().equals(user.getFacultyId())) {
            return true;
        }

        return portfolioShareRepository.existsByPortfolioAndSharedWith(item.getPortfolio(), user);
    }

    public List<PortfolioItem> getItemsByPortfolioId(Long portfolioId) {
        try {
            return portfolioItemRepository.findByPortfolioId(portfolioId);
        } catch (Exception e) {
            log.error("Error getting items by portfolio ID: ", e);
            return Collections.emptyList();
        }
    }
}