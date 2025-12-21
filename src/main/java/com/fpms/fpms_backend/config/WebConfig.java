package com.fpms.fpms_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path to uploads directory
        String absolutePath = new File(uploadDir).getAbsolutePath();

        log.info("Configuring static resource handler for uploads");
        log.info("Upload directory: {}", absolutePath);
        log.info("Mapping: /uploads/** -> file:{}/", absolutePath);

        // Map /uploads/** to the actual uploads directory
        // Add trailing slash to the file: URL
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(0); // Disable cache for development

        // Log the configuration
        log.info("Static resources configured:");
        log.info("- /uploads/** -> file:{}/", absolutePath);
        log.info("- /** -> classpath:/static/");
    }
}