package com.fpms.fpms_backend.controller;

import com.fpms.fpms_backend.model.entities.*;
import com.fpms.fpms_backend.model.enums.FacultyRole;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.*;
import com.fpms.fpms_backend.repository.PortfolioItemRepository;
import com.fpms.fpms_backend.repository.PortfolioRepository;
import com.fpms.fpms_backend.repository.SystemCredentialsRepository;
import com.fpms.fpms_backend.service.PortfolioService;
import com.fpms.fpms_backend.service.UserDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/portfolio")
@RequiredArgsConstructor
@Slf4j
public class PortfolioController {
    private final PortfolioService portfolioService;
    private final FacultyRepository facultyRepository;
    private final SystemCredentialsRepository systemCredentialsRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserDocumentService userDocumentService;

    @GetMapping
    public String portfolioPage(Model model) {
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("User not authenticated when accessing portfolio page");
                return "redirect:/login";
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("pageTitle", "Portfolios");

            // Get user's personal portfolios
            List<Portfolio> myPortfolios = portfolioService.getUserPortfolios();
            log.debug("Found {} personal portfolios for user {}", myPortfolios.size(), currentUser.getFacultyId());
            model.addAttribute("myPortfolios", myPortfolios);

            // Get shared portfolios
            List<PortfolioShare> sharedPortfolios = portfolioService.getSharedWithMe();
            log.debug("Found {} shared portfolios for user {}", sharedPortfolios.size(), currentUser.getFacultyId());
            model.addAttribute("sharedPortfolios", sharedPortfolios);

            return "common/portfolios";

        } catch (Exception e) {
            log.error("Error loading portfolio page: ", e);
            model.addAttribute("error", "Error loading portfolios: " + e.getMessage());
            return "common/portfolios";
        }
    }

    @GetMapping("/details/{portfolioId}")
    public String viewPortfolio(@PathVariable Long portfolioId,
                                @RequestParam(required = false) Long folderId,
                                Model model) {
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                log.warn("User not authenticated when accessing portfolio {}", portfolioId);
                return "redirect:/login";
            }

            Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
            if (portfolio == null) {
                model.addAttribute("error", "Portfolio not found");
                return "redirect:/portfolio";
            }

            // Check access - FIXED: Direct access check
            boolean hasAccess = checkPortfolioAccess(currentUser, portfolio);
            if (!hasAccess) {
                model.addAttribute("error", "Access denied to this portfolio");
                return "redirect:/portfolio";
            }

            // Get portfolio items
            List<PortfolioItem> items = portfolioService.getPortfolioItems(portfolioId, folderId);
            log.debug("Found {} items in portfolio {} (folder: {})", items.size(), portfolioId, folderId);

            // Get current folder path
            List<PortfolioItem> folderPath = new ArrayList<>();
            if (folderId != null && folderId > 0) {
                PortfolioItem currentFolder = portfolioItemRepository.findById(folderId).orElse(null);
                if (currentFolder != null && currentFolder.isFolder()) {
                    PortfolioItem tempFolder = currentFolder;
                    while (tempFolder != null) {
                        folderPath.add(0, tempFolder);
                        tempFolder = tempFolder.getParentFolder();
                    }
                }
            }

            model.addAttribute("portfolio", portfolio);
            model.addAttribute("portfolioId", portfolioId);
            model.addAttribute("folderId", folderId);
            model.addAttribute("items", items != null ? items : new ArrayList<>());
            model.addAttribute("currentFolderPath", folderPath);
            model.addAttribute("user", currentUser);
            model.addAttribute("pageTitle", portfolio.getName());

            log.info("Successfully loaded portfolio {} for user {}", portfolioId, currentUser.getFacultyId());
            return "common/portfolio-view";

        } catch (Exception e) {
            log.error("Error viewing portfolio {}: ", portfolioId, e);
            model.addAttribute("error", "Error loading portfolio: " + e.getMessage());
            return "redirect:/portfolio";
        }
    }

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPortfolio(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String type) {

        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            // Convert string to enum with error handling
            Portfolio.PortfolioType portfolioType;
            try {
                portfolioType = Portfolio.PortfolioType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("Invalid portfolio type: {}", type);
                response.put("success", false);
                response.put("message", "Invalid portfolio type. Valid types: PERSONAL, DEPARTMENT, COLLEGE");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate user role for portfolio type
            if (portfolioType == Portfolio.PortfolioType.DEPARTMENT && currentUser.getRole() != FacultyRole.DEPT_HEAD) {
                response.put("success", false);
                response.put("message", "Only department heads can create department portfolios");
                return ResponseEntity.badRequest().body(response);
            }

            if (portfolioType == Portfolio.PortfolioType.COLLEGE && currentUser.getRole() != FacultyRole.DEAN) {
                response.put("success", false);
                response.put("message", "Only deans can create college portfolios");
                return ResponseEntity.badRequest().body(response);
            }

            Portfolio portfolio = portfolioService.createPortfolio(name, description, portfolioType);
            response.put("success", true);
            response.put("portfolio", portfolio);
            response.put("message", "Portfolio created successfully");
            log.info("Portfolio created: {} by user {}", name, currentUser.getFacultyId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating portfolio: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/create-folder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createFolder(
            @RequestParam String folderName,
            @RequestParam Long portfolioId,
            @RequestParam(required = false) Long parentFolderId) {

        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            Portfolio portfolio = portfolioService.getPortfolioById(portfolioId);
            if (portfolio == null) {
                response.put("success", false);
                response.put("message", "Portfolio not found");
                return ResponseEntity.badRequest().body(response);
            }

            // Check access - FIXED: Direct access check
            boolean hasAccess = checkPortfolioAccess(currentUser, portfolio);
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "Access denied to this portfolio");
                return ResponseEntity.badRequest().body(response);
            }

            PortfolioItem folder = new PortfolioItem();
            folder.setName(folderName);
            folder.setFolder(true);
            folder.setPortfolio(portfolio);
            folder.setUploadedBy(currentUser);
            folder.setStatus("APPROVED");
            folder.setUploadedAt(LocalDateTime.now());

            if (parentFolderId != null && parentFolderId > 0) {
                PortfolioItem parentFolder = portfolioItemRepository.findById(parentFolderId).orElse(null);
                if (parentFolder == null || !parentFolder.isFolder()) {
                    response.put("success", false);
                    response.put("message", "Parent folder not found or invalid");
                    return ResponseEntity.badRequest().body(response);
                }
                folder.setParentFolder(parentFolder);
            }

            PortfolioItem savedFolder = portfolioItemRepository.save(folder);

            response.put("success", true);
            response.put("folder", savedFolder);
            response.put("message", "Folder created successfully");
            log.info("Folder created: {} in portfolio {}", folderName, portfolioId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating folder: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // FIXED: Changed parameter name to match JavaScript
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @RequestParam("file") MultipartFile[] files,
            @RequestParam Long portfolioId,
            @RequestParam(required = false) Long folderId) {

        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Uploading {} files to portfolio {} (folder: {})", files.length, portfolioId, folderId);

            int successCount = 0;
            int failCount = 0;
            List<String> successFiles = new ArrayList<>();
            List<String> failedFiles = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    portfolioService.uploadFile(file, portfolioId, folderId, "");
                    successCount++;
                    successFiles.add(file.getOriginalFilename());
                    log.info("Successfully uploaded: {}", file.getOriginalFilename());
                } catch (Exception e) {
                    failCount++;
                    failedFiles.add(file.getOriginalFilename() + ": " + e.getMessage());
                    log.error("Failed to upload file {}: {}", file.getOriginalFilename(), e.getMessage());
                }
            }

            if (successCount > 0) {
                response.put("success", true);
                response.put("message", "Successfully uploaded " + successCount + " file(s)");
                response.put("successFiles", successFiles);
                response.put("successCount", successCount);

                if (!failedFiles.isEmpty()) {
                    response.put("warnings", failedFiles);
                    response.put("failCount", failCount);
                }
                log.info("Upload successful: {} files uploaded to portfolio {}", successCount, portfolioId);
            } else {
                response.put("success", false);
                response.put("message", "All uploads failed");
                response.put("errors", failedFiles);
                log.error("All uploads failed for portfolio {}", portfolioId);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading files: ", e);
            response.put("success", false);
            response.put("message", "Upload error: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{portfolioId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePortfolio(@PathVariable Long portfolioId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            portfolioService.deletePortfolio(portfolioId);
            response.put("success", true);
            response.put("message", "Portfolio deleted successfully");
            log.info("Portfolio {} deleted by user {}", portfolioId, currentUser.getFacultyId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting portfolio {}: ", portfolioId, e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/my-portfolios")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMyPortfoliosApi() {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            List<Portfolio> portfolios = portfolioService.getUserPortfolios();
            log.debug("API: Found {} portfolios for user {}", portfolios.size(), currentUser.getFacultyId());

            List<Map<String, Object>> portfolioDTOs = new ArrayList<>();
            for (Portfolio portfolio : portfolios) {
                Map<String, Object> portfolioData = new HashMap<>();
                portfolioData.put("id", portfolio.getId());
                portfolioData.put("name", portfolio.getName());
                portfolioData.put("description", portfolio.getDescription());
                portfolioData.put("type", portfolio.getType().name());
                portfolioData.put("createdAt", portfolio.getCreatedAt());
                portfolioData.put("itemCount", portfolio.getItems() != null ? portfolio.getItems().size() : 0);
                portfolioData.put("ownerName", portfolio.getOwner().getFirstName() + " " + portfolio.getOwner().getLastName());
                portfolioDTOs.add(portfolioData);
            }

            response.put("success", true);
            response.put("portfolios", portfolioDTOs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting user portfolios: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/department-portfolios")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDepartmentPortfoliosApi() {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            List<Portfolio> portfolios = portfolioService.getDepartmentPortfolios();
            log.debug("API: Found {} department portfolios for user {}", portfolios.size(), currentUser.getFacultyId());

            List<Map<String, Object>> portfolioDTOs = new ArrayList<>();
            for (Portfolio portfolio : portfolios) {
                Map<String, Object> portfolioData = new HashMap<>();
                portfolioData.put("id", portfolio.getId());
                portfolioData.put("name", portfolio.getName());
                portfolioData.put("description", portfolio.getDescription());
                portfolioData.put("type", portfolio.getType().name());
                portfolioData.put("createdAt", portfolio.getCreatedAt());
                portfolioData.put("itemCount", portfolio.getItems() != null ? portfolio.getItems().size() : 0);
                portfolioData.put("ownerName", portfolio.getOwner().getFirstName() + " " + portfolio.getOwner().getLastName());
                portfolioData.put("departmentName", portfolio.getDepartment() != null ? portfolio.getDepartment().getDeptName() : "N/A");
                portfolioDTOs.add(portfolioData);
            }

            response.put("success", true);
            response.put("portfolios", portfolioDTOs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting department portfolios: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/college-portfolios")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCollegePortfoliosApi() {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if user is DEAN
            if (currentUser.getRole() != FacultyRole.DEAN) {
                response.put("success", false);
                response.put("message", "Access denied. Only deans can access college portfolios.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Use the existing repository method
            List<Portfolio> collegePortfolios = portfolioRepository.findCollegePortfolios();

            List<Map<String, Object>> portfolioDTOs = new ArrayList<>();
            for (Portfolio portfolio : collegePortfolios) {
                Map<String, Object> portfolioData = new HashMap<>();
                portfolioData.put("id", portfolio.getId());
                portfolioData.put("name", portfolio.getName());
                portfolioData.put("description", portfolio.getDescription());
                portfolioData.put("type", portfolio.getType().name());
                portfolioData.put("createdAt", portfolio.getCreatedAt());
                portfolioData.put("itemCount", portfolio.getItems() != null ? portfolio.getItems().size() : 0);
                portfolioData.put("ownerName", portfolio.getOwner().getFirstName() + " " + portfolio.getOwner().getLastName());
                portfolioDTOs.add(portfolioData);
            }

            response.put("success", true);
            response.put("portfolios", portfolioDTOs);
            log.debug("API: Found {} college portfolios for dean {}", collegePortfolios.size(), currentUser.getFacultyId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting college portfolios: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/folders/{portfolioId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFolders(@PathVariable Long portfolioId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            List<PortfolioItem> allItems = portfolioItemRepository.findByPortfolioId(portfolioId);

            List<PortfolioItem> folders = allItems.stream()
                    .filter(PortfolioItem::isFolder)
                    .collect(Collectors.toList());

            List<Map<String, Object>> folderDTOs = new ArrayList<>();
            for (PortfolioItem folder : folders) {
                Map<String, Object> folderData = new HashMap<>();
                folderData.put("id", folder.getId());
                folderData.put("name", folder.getName());
                folderData.put("parentFolderId", folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
                folderDTOs.add(folderData);
            }

            response.put("success", true);
            response.put("folders", folderDTOs);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting folders: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/download/{itemId}")
    public ResponseEntity<?> downloadFile(@PathVariable Long itemId) {
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            PortfolioItem item = portfolioItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // FIXED: Check access using helper method
            boolean hasAccess = checkItemAccess(currentUser, item);
            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied to this file");
            }

            String filePath = item.getFilePath();
            if (filePath == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File path not found");
            }

            File file = new File(filePath);

            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found on server");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + item.getName() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            log.info("File downloaded: {} by user {}", item.getName(), currentUser.getFacultyId());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .body(new FileSystemResource(file));

        } catch (Exception e) {
            log.error("Error downloading file: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error downloading file: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-item/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                throw new RuntimeException("User not authenticated");
            }

            PortfolioItem item = portfolioItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            // Check permission
            boolean canDelete = false;
            if (item.getPortfolio() == null) {
                // User document - only uploader can delete
                canDelete = item.getUploadedBy() != null &&
                        item.getUploadedBy().getFacultyId().equals(currentUser.getFacultyId());
            } else {
                // Portfolio item - owner or uploader can delete
                canDelete = item.getUploadedBy().getFacultyId().equals(currentUser.getFacultyId()) ||
                        item.getPortfolio().getOwner().getFacultyId().equals(currentUser.getFacultyId());
            }

            if (!canDelete) {
                throw new RuntimeException("You don't have permission to delete this item");
            }

            // Delete file from storage if it's a file
            if (!item.isFolder() && item.getFilePath() != null) {
                File file = new File(item.getFilePath());
                if (file.exists()) {
                    file.delete();
                    log.info("Deleted file from storage: {}", item.getFilePath());
                }
            }

            portfolioItemRepository.delete(item);

            response.put("success", true);
            response.put("message", "Item deleted successfully");
            log.info("Item {} deleted by user {}", itemId, currentUser.getFacultyId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting item: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/item/{itemId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getItemDetails(@PathVariable Long itemId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            PortfolioItem item = portfolioItemRepository.findById(itemId)
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            // FIXED: Check access using helper method
            boolean hasAccess = checkItemAccess(currentUser, item);
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "Access denied");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> itemData = new HashMap<>();
            itemData.put("id", item.getId());
            itemData.put("name", item.getName());
            itemData.put("isFolder", item.isFolder());
            itemData.put("fileType", item.getFileType());
            itemData.put("fileSize", item.getFileSize());
            itemData.put("filePath", item.getFilePath());
            itemData.put("status", item.getStatus());
            itemData.put("uploadedAt", item.getUploadedAt());

            if (item.getUploadedBy() != null) {
                Map<String, Object> uploadedBy = new HashMap<>();
                uploadedBy.put("id", item.getUploadedBy().getFacultyId());
                uploadedBy.put("name", item.getUploadedBy().getFirstName() + " " + item.getUploadedBy().getLastName());
                uploadedBy.put("email", item.getUploadedBy().getEmail());
                itemData.put("uploadedBy", uploadedBy);
            }

            response.put("success", true);
            response.put("item", itemData);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting item details: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/folder/{folderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFolderItems(@PathVariable Long folderId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            PortfolioItem folder = portfolioItemRepository.findById(folderId)
                    .filter(PortfolioItem::isFolder)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            // FIXED: Check access using helper method
            boolean hasAccess = checkItemAccess(currentUser, folder);
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "Access denied");
                return ResponseEntity.badRequest().body(response);
            }

            List<PortfolioItem> items = portfolioItemRepository.findByParentFolder(folder);

            List<Map<String, Object>> itemDTOs = items.stream()
                    .map(item -> {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("id", item.getId());
                        itemData.put("name", item.getName());
                        itemData.put("isFolder", item.isFolder());
                        itemData.put("fileType", item.getFileType());
                        itemData.put("fileSize", item.getFileSize());
                        itemData.put("status", item.getStatus());
                        itemData.put("uploadedAt", item.getUploadedAt());
                        return itemData;
                    })
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("items", itemDTOs);
            response.put("folderName", folder.getName());
            response.put("portfolioId", folder.getPortfolio() != null ? folder.getPortfolio().getId() : null);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting folder items: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/user-documents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDocuments() {
        Map<String, Object> response = new HashMap<>();

        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> documents = userDocumentService.getAllUserDocuments(
                    currentUser.getFacultyId());

            response.put("success", true);
            response.put("documents", documents);
            response.put("totalDocuments", documents.size());
            log.debug("API: Found {} user documents for user {}", documents.size(), currentUser.getFacultyId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting user documents: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/upload-to-documents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadToDocuments(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "personal") String category) {

        Map<String, Object> response = new HashMap<>();

        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> uploadResult = userDocumentService.uploadToUserDocuments(
                    file, currentUser.getFacultyId(), category);

            return ResponseEntity.ok(uploadResult);

        } catch (Exception e) {
            log.error("Error uploading to documents: ", e);
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/api/upload-multiple-to-documents")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadMultipleToDocuments(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(defaultValue = "personal") String category) {

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> uploadResults = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            for (MultipartFile file : files) {
                try {
                    Map<String, Object> uploadResult = userDocumentService.uploadToUserDocuments(
                            file, currentUser.getFacultyId(), category);

                    if ((Boolean) uploadResult.get("success")) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                    uploadResults.add(uploadResult);
                } catch (Exception e) {
                    failCount++;
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("success", false);
                    errorResult.put("fileName", file.getOriginalFilename());
                    errorResult.put("message", "Upload failed: " + e.getMessage());
                    uploadResults.add(errorResult);
                }
            }

            response.put("success", successCount > 0);
            response.put("message", String.format("Uploaded %d files successfully, %d failed", successCount, failCount));
            response.put("successCount", successCount);
            response.put("failCount", failCount);
            response.put("results", uploadResults);

            log.info("Multiple documents upload: {} success, {} fail for user {}", successCount, failCount, currentUser.getFacultyId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error uploading multiple files to documents: ", e);
            response.put("success", false);
            response.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/api/delete-document/{documentId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUserDocument(@PathVariable Long documentId) {
        Map<String, Object> response = new HashMap<>();

        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> deleteResult = userDocumentService.deleteUserDocument(
                    documentId, currentUser.getFacultyId());

            return ResponseEntity.ok(deleteResult);

        } catch (Exception e) {
            log.error("Error deleting document: ", e);
            response.put("success", false);
            response.put("message", "Delete failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/document-stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserDocumentStats() {
        Map<String, Object> response = new HashMap<>();

        try {
            Faculty currentUser = getCurrentUser();
            if (currentUser == null) {
                response.put("success", false);
                response.put("message", "User not authenticated");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> stats = userDocumentService.getUserDocumentStats(
                    currentUser.getFacultyId());

            response.put("success", true);
            response.put("stats", stats);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting document stats: ", e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Helper methods for access control
    private boolean checkPortfolioAccess(Faculty user, Portfolio portfolio) {
        if (portfolio == null || user == null) {
            return false;
        }

        // Owner always has access
        if (portfolio.getOwner() != null &&
                portfolio.getOwner().getFacultyId().equals(user.getFacultyId())) {
            return true;
        }

        // Check if portfolio is shared with user
        if (portfolio.getShares() != null) {
            boolean isShared = portfolio.getShares().stream()
                    .anyMatch(share -> share.getSharedWith() != null &&
                            share.getSharedWith().getFacultyId().equals(user.getFacultyId()));
            if (isShared) {
                return true;
            }
        }

        // Check department access for department portfolios
        if (portfolio.getType() == Portfolio.PortfolioType.DEPARTMENT &&
                portfolio.getDepartment() != null &&
                user.getDepartment() != null) {

            // FIXED: Compare department codes (deptCode is String, not getId())
            String portfolioDeptCode = portfolio.getDepartment().getDeptCode();
            String userDeptCode = user.getDepartment().getDeptCode();

            if (portfolioDeptCode != null && userDeptCode != null &&
                    portfolioDeptCode.equals(userDeptCode)) {

                // Department heads have full access
                if (user.getRole() == FacultyRole.DEPT_HEAD) {
                    return true;
                }
                // Regular faculty in same department might have read access
                // Add your department access logic here
                return true; // Assuming all department faculty can access department portfolios
            }
        }

        // Check college access for college portfolios
        if (portfolio.getType() == Portfolio.PortfolioType.COLLEGE &&
                user.getRole() == FacultyRole.DEAN) {
            return true;
        }

        return false;
    }

    private boolean checkItemAccess(Faculty user, PortfolioItem item) {
        if (item == null || user == null) {
            return false;
        }

        // For user documents (no portfolio)
        if (item.getPortfolio() == null) {
            // Only the uploader can access
            return item.getUploadedBy() != null &&
                    item.getUploadedBy().getFacultyId().equals(user.getFacultyId());
        }

        // For portfolio items, check portfolio access
        return checkPortfolioAccess(user, item.getPortfolio());
    }

    private Faculty getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
                log.debug("No authenticated user found");
                return null;
            }

            String username = auth.getName();
            log.debug("Getting current user for username: {}", username);

            // Try to find by username in system credentials
            Optional<SystemCredentials> credentials = systemCredentialsRepository.findByUsername(username);
            if (credentials.isPresent() && credentials.get().getFaculty() != null) {
                log.debug("Found user via system credentials: {}", username);
                return credentials.get().getFaculty();
            }

            // Try to find by email
            Optional<Faculty> facultyOpt = facultyRepository.findByEmail(username);
            if (facultyOpt.isPresent()) {
                log.debug("Found user via email: {}", username);
                return facultyOpt.get();
            }

            log.warn("User not found for username/email: {}", username);
            return null;
        } catch (Exception e) {
            log.error("Error getting current user: ", e);
            return null;
        }
    }
}