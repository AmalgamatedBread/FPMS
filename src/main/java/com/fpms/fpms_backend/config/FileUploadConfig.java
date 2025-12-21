package com.fpms.fpms_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
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
                // Use absolute path to avoid confusion
                String absolutePath = new File(uploadDir).getAbsolutePath();
                Path uploadPath = Paths.get(absolutePath);

                log.info("Upload directory configured at: {}", absolutePath);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
                }

                // Create users subdirectory
                Path usersPath = uploadPath.resolve("users");
                if (!Files.exists(usersPath)) {
                    Files.createDirectories(usersPath);
                    log.info("Created users directory: {}", usersPath.toAbsolutePath());
                }

                // Create other necessary directories
                String[] subDirs = {"temp", "documents", "portfolios"};
                for (String subDir : subDirs) {
                    Path dirPath = uploadPath.resolve(subDir);
                    if (!Files.exists(dirPath)) {
                        Files.createDirectories(dirPath);
                        log.info("Created directory: {}", dirPath.toAbsolutePath());
                    }
                }

            } catch (Exception e) {
                log.error("Failed to create upload directories: ", e);
                throw new RuntimeException("Failed to create upload directories", e);
            }
        };
    }
}