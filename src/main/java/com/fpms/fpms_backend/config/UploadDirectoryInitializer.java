package com.fpms.fpms_backend.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class UploadDirectoryInitializer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            log.info("Initializing upload directories...");

            // Create main upload directory
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }

            // Create users directory
            Path usersPath = Paths.get(uploadDir + "/users");
            if (!Files.exists(usersPath)) {
                Files.createDirectories(usersPath);
                log.info("Created users directory: {}", usersPath.toAbsolutePath());
            }

            // Create temp directory
            Path tempPath = Paths.get(uploadDir + "/temp");
            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                log.info("Created temp directory: {}", tempPath.toAbsolutePath());
            }

            log.info("Upload directories initialized successfully");

        } catch (IOException e) {
            log.error("Failed to create upload directories: ", e);
            throw new RuntimeException("Failed to create upload directories", e);
        }
    }
}