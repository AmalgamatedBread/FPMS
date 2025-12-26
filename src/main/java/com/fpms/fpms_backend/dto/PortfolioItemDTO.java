package com.fpms.fpms_backend.dto;

import com.fpms.fpms_backend.model.entities.PortfolioItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItemDTO {
    private Long id;
    private String name;
    private boolean folder;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String formattedSize;
    private String status;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private String formattedDate;
    private String iconClass;
    private String badgeColor;
    private Long portfolioId;
    private Long parentFolderId;

    public static PortfolioItemDTO fromEntity(PortfolioItem item) {
        if (item == null) {
            return null;
        }

        PortfolioItemDTO dto = new PortfolioItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setFolder(item.isFolder());
        dto.setFilePath(item.getFilePath());
        dto.setFileType(item.getFileType());
        dto.setFileSize(item.getFileSize());
        dto.setStatus(item.getStatus());

        // Format file size
        if (item.getFileSize() != null) {
            dto.setFormattedSize(formatFileSize(item.getFileSize()));
        }

        // Set uploaded by info
        if (item.getUploadedBy() != null) {
            dto.setUploadedBy(item.getUploadedBy().getFirstName() + " " + item.getUploadedBy().getLastName());
        }

        // Format dates
        if (item.getUploadedAt() != null) {
            dto.setUploadedAt(item.getUploadedAt());
            dto.setFormattedDate(formatDate(item.getUploadedAt()));
        }

        // Set portfolio info
        if (item.getPortfolio() != null) {
            dto.setPortfolioId(item.getPortfolio().getId());
        }

        // Set parent folder info
        if (item.getParentFolder() != null) {
            dto.setParentFolderId(item.getParentFolder().getId());
        }

        // Set icon and badge colors
        dto.setIconClass(getFileIcon(item));
        dto.setBadgeColor(getStatusColor(item.getStatus()));

        return dto;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static String formatDate(LocalDateTime date) {
        if (date == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return date.format(formatter);
    }

    private static String getFileIcon(PortfolioItem item) {
        if (item.isFolder()) {
            return "fas fa-folder";
        }

        if (item.getFileType() == null) {
            return "fas fa-file";
        }

        String type = item.getFileType().toLowerCase();
        if (type.contains("pdf")) return "fas fa-file-pdf";
        if (type.contains("word") || type.contains("document")) return "fas fa-file-word";
        if (type.contains("excel") || type.contains("sheet")) return "fas fa-file-excel";
        if (type.contains("image")) return "fas fa-file-image";
        if (type.contains("zip") || type.contains("rar") || type.contains("7z")) return "fas fa-file-archive";
        if (type.contains("text") || type.contains("plain")) return "fas fa-file-alt";

        return "fas fa-file";
    }

    private static String getStatusColor(String status) {
        if (status == null) return "secondary";
        switch (status.toUpperCase()) {
            case "APPROVED": return "success";
            case "PENDING": return "warning";
            case "REJECTED": return "danger";
            default: return "secondary";
        }
    }
}