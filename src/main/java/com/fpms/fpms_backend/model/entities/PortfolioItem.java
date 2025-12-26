package com.fpms.fpms_backend.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_folder", nullable = false)
    private boolean isFolder = false;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_id", length = 50, unique = true)
    private String fileId;  // UUID for unique file identification

    @Column(length = 500)
    private String comments = "";

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;  // REMOVED nullable = false

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private Faculty uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_folder_id")
    private PortfolioItem parentFolder;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        fileId = java.util.UUID.randomUUID().toString().substring(0, 13); // Generate unique file ID

        if (status == null) {
            status = "PENDING";
        }
        if (comments == null) {
            comments = "";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to check if it's a file (not a folder)
    public boolean isFile() {
        return !isFolder;
    }

    // Get file extension
    public String getFileExtension() {
        if (isFolder || name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    // Get formatted file size
    public String getFormattedSize() {
        if (isFolder || fileSize == null) return "";

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    // Get icon class based on file type
    public String getIconClass() {
        if (isFolder) return "fas fa-folder";

        String ext = getFileExtension().toLowerCase();
        switch (ext) {
            case "pdf": return "fas fa-file-pdf";
            case "doc":
            case "docx": return "fas fa-file-word";
            case "xls":
            case "xlsx": return "fas fa-file-excel";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif": return "fas fa-file-image";
            case "zip":
            case "rar": return "fas fa-file-archive";
            case "txt": return "fas fa-file-alt";
            default: return "fas fa-file";
        }
    }

    // Get status badge color
    public String getBadgeColor() {
        if (status == null) return "secondary";
        switch (status.toUpperCase()) {
            case "APPROVED": return "success";
            case "PENDING": return "warning";
            case "REJECTED": return "danger";
            default: return "secondary";
        }
    }

    @Override
    public String toString() {
        return "PortfolioItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isFolder=" + isFolder +
                ", fileId='" + fileId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}