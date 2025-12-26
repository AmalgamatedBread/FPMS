package com.fpms.fpms_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FileUploadConfig {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Bean
    public CommandLineRunner initUploadDirectories() {
        return args -> {
            try {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

                log.info("=== FILE UPLOAD CONFIGURATION ===");
                log.info("Upload base directory: {}", uploadPath);

                // Create main upload directory
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("✅ Created main upload directory: {}", uploadPath);
                }

                // Create all subdirectories
                String[] subDirs = {
                        "backups",
                        "documents",           // Main documents directory
                        "portfolios",          // Portfolio metadata
                        "profile-photos",      // User profile pictures
                        "temp",                // Temporary uploads
                        "users"                // User-specific directories (legacy)
                };

                for (String subDir : subDirs) {
                    Path dirPath = uploadPath.resolve(subDir);
                    if (!Files.exists(dirPath)) {
                        Files.createDirectories(dirPath);
                        log.info("✅ Created directory: {}", dirPath);
                    }
                }

                // Verify write permissions
                Path testFile = uploadPath.resolve("documents").resolve("test-write.txt");
                Files.writeString(testFile, "Test write at " + System.currentTimeMillis());
                Files.deleteIfExists(testFile);
                log.info("✅ Write test successful");

                log.info("=== UPLOAD DIRECTORIES READY ===");

            } catch (Exception e) {
                log.error("❌ Failed to create upload directories", e);
                throw new RuntimeException("Failed to initialize upload directories: " + e.getMessage(), e);
            }
        };
    }
}