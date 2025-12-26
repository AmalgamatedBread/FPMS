package com.fpms.fpms_backend.service;

import com.fpms.fpms_backend.model.entities.Faculty;
import com.fpms.fpms_backend.model.entities.PortfolioItem;
import com.fpms.fpms_backend.repository.FacultyRepository;
import com.fpms.fpms_backend.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDocumentService {
    private final FacultyRepository facultyRepository;
    private final PortfolioItemRepository portfolioItemRepository;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public Path getUserDocumentsDirectory(Long userId) throws IOException {
        String path = String.format("%s/documents/user_%d", uploadDir, userId);
        Path userDir = Paths.get(path).toAbsolutePath();

        if (!Files.exists(userDir)) {
            Files.createDirectories(userDir);
            log.info("Created user documents directory: {}", userDir);
        }
        return userDir;
    }

    public List<Map<String, Object>> getAllUserDocuments(Long userId) {
        try {
            Faculty user = facultyRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Get documents where portfolio is null (user documents)
            List<PortfolioItem> userDocuments = portfolioItemRepository.findByUploadedByAndPortfolioIsNull(user);

            return userDocuments.stream()
                    .map(item -> {
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("id", item.getId());
                        doc.put("name", item.getName());
                        doc.put("fileType", item.getFileType());
                        doc.put("fileSize", item.getFileSize());
                        doc.put("formattedSize", formatFileSize(item.getFileSize()));
                        doc.put("fileId", item.getFileId());
                        doc.put("uploadedAt", item.getUploadedAt());
                        doc.put("status", item.getStatus());
                        doc.put("filePath", item.getFilePath());

                        // Determine category based on file path
                        String category = "personal";
                        if (item.getFilePath() != null) {
                            if (item.getFilePath().contains("/work/")) category = "work";
                            else if (item.getFilePath().contains("/archive/")) category = "archive";
                            else if (item.getFilePath().contains("/shared/")) category = "shared";
                        }
                        doc.put("category", category);
                        doc.put("icon", getFileIcon(item));

                        return doc;
                    })
                    .sorted((a, b) -> ((LocalDateTime) b.get("uploadedAt")).compareTo((LocalDateTime) a.get("uploadedAt")))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting user documents: ", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public Map<String, Object> uploadToUserDocuments(MultipartFile file, Long userId, String category) throws IOException {
        Map<String, Object> result = new HashMap<>();

        try {
            Faculty user = facultyRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            long maxSize = 50 * 1024 * 1024; // 50MB
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size exceeds 50MB limit");
            }

            if (file.getContentType() != null && file.getContentType().startsWith("video/")) {
                throw new IllegalArgumentException("Video files are not allowed");
            }

            // Validate file extension
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new IllegalArgumentException("File name is empty");
            }

            String fileExtension = getFileExtension(originalFilename);
            String[] allowedExtensions = {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
                    ".jpg", ".jpeg", ".png", ".gif", ".txt", ".zip", ".rar"};
            if (!Arrays.asList(allowedExtensions).contains(fileExtension.toLowerCase())) {
                throw new IllegalArgumentException("File type not allowed. Allowed types: " + Arrays.toString(allowedExtensions));
            }

            Path userDir = getUserDocumentsDirectory(userId);

            String subDir = switch (category.toLowerCase()) {
                case "work" -> "work";
                case "archive" -> "archive";
                case "shared" -> "shared";
                default -> "personal";
            };

            Path targetDir = userDir.resolve(subDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String fileId = UUID.randomUUID().toString().substring(0, 13);
            String uniqueFilename = String.format("%s_%d%s",
                    fileId,
                    System.currentTimeMillis() / 1000,
                    fileExtension);

            Path filePath = targetDir.resolve(uniqueFilename);

            // Ensure parent directory exists
            Files.createDirectories(filePath.getParent());

            // Copy file to target location
            Files.copy(file.getInputStream(), filePath);

            PortfolioItem item = new PortfolioItem();
            item.setName(originalFilename);
            item.setFilePath(filePath.toString());
            item.setFileType(file.getContentType());
            item.setFileSize(file.getSize());
            item.setFileId(fileId);
            item.setFolder(false);

            // IMPORTANT: Set portfolio to null for user documents
            item.setPortfolio(null);

            item.setUploadedBy(user);
            item.setUploadedAt(LocalDateTime.now());
            item.setStatus("APPROVED");
            item.setComments("Uploaded to " + subDir + " documents");
            item.setUpdatedAt(LocalDateTime.now());

            PortfolioItem savedItem = portfolioItemRepository.save(item);

            result.put("success", true);
            result.put("message", "File uploaded to " + subDir + " documents");
            result.put("fileId", savedItem.getFileId());
            result.put("fileName", savedItem.getName());
            result.put("filePath", savedItem.getFilePath());
            result.put("category", subDir);

            log.info("File uploaded to user documents: {} -> {}", originalFilename, filePath);

        } catch (Exception e) {
            log.error("Error uploading to user documents: ", e);
            result.put("success", false);
            result.put("message", "Upload failed: " + e.getMessage());
            throw e; // Re-throw to trigger rollback
        }

        return result;
    }

    @Transactional
    public Map<String, Object> deleteUserDocument(Long documentId, Long userId) throws IOException {
        Map<String, Object> result = new HashMap<>();
        try {
            PortfolioItem item = portfolioItemRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("Document not found"));

            // Check if it's a user document (portfolio is null) and user owns it
            if (item.getPortfolio() != null || !item.getUploadedBy().getFacultyId().equals(userId)) {
                throw new IllegalStateException("You don't have permission to delete this document");
            }

            if (item.getFilePath() != null) {
                Path filePath = Paths.get(item.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("Deleted file: {}", filePath);
                }
            }

            portfolioItemRepository.delete(item);

            result.put("success", true);
            result.put("message", "Document deleted successfully");

        } catch (Exception e) {
            log.error("Error deleting user document: ", e);
            result.put("success", false);
            result.put("message", "Delete failed: " + e.getMessage());
            throw e;
        }
        return result;
    }

    public Map<String, Object> getUserDocumentStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            Faculty user = facultyRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            // Get user documents where portfolio is null
            List<PortfolioItem> userDocuments = portfolioItemRepository.findByUploadedByAndPortfolioIsNull(user);

            long totalFiles = userDocuments.size();
            long totalSize = userDocuments.stream()
                    .filter(item -> item.getFileSize() != null)
                    .mapToLong(PortfolioItem::getFileSize)
                    .sum();

            Map<String, Long> filesByCategory = new HashMap<>();
            filesByCategory.put("personal", 0L);
            filesByCategory.put("work", 0L);
            filesByCategory.put("archive", 0L);
            filesByCategory.put("shared", 0L);

            for (PortfolioItem item : userDocuments) {
                String category = "personal";
                if (item.getFilePath() != null) {
                    if (item.getFilePath().contains("/work/")) category = "work";
                    else if (item.getFilePath().contains("/archive/")) category = "archive";
                    else if (item.getFilePath().contains("/shared/")) category = "shared";
                }
                filesByCategory.put(category, filesByCategory.get(category) + 1);
            }

            stats.put("totalFiles", totalFiles);
            stats.put("totalSize", totalSize);
            stats.put("formattedSize", formatFileSize(totalSize));
            stats.put("filesByCategory", filesByCategory);
            stats.put("lastUpdated", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error getting user stats: ", e);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String getFileIcon(PortfolioItem item) {
        if (item.getFileType() == null) return "fas fa-file";
        String type = item.getFileType().toLowerCase();
        if (type.contains("pdf")) return "fas fa-file-pdf";
        if (type.contains("word") || type.contains("document")) return "fas fa-file-word";
        if (type.contains("excel") || type.contains("sheet")) return "fas fa-file-excel";
        if (type.contains("powerpoint") || type.contains("presentation")) return "fas fa-file-powerpoint";
        if (type.contains("image")) return "fas fa-file-image";
        if (type.contains("zip") || type.contains("rar")) return "fas fa-file-archive";
        if (type.contains("text") || type.contains("plain")) return "fas fa-file-alt";
        return "fas fa-file";
    }
}