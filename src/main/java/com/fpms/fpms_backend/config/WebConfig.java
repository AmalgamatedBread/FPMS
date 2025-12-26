package com.fpms.fpms_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // Get absolute normalized path for cross-platform compatibility
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            log.info("=== STATIC RESOURCE CONFIGURATION ===");
            log.info("Upload directory path: {}", uploadPath);
            log.info("Resource handler mapping: /uploads/** -> file:{}/", uploadPath);

            // Configure uploads directory
            String resourceLocation = "file:" + uploadPath.toString() + "/";

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(resourceLocation)
                    .setCachePeriod(3600) // Cache for 1 hour
                    .resourceChain(true);

            // ===== ADD THESE LINES =====
            // Configure CSS, JS, and Images from classpath
            registry.addResourceHandler("/css/**")
                    .addResourceLocations("classpath:/static/css/")
                    .setCachePeriod(3600);

            registry.addResourceHandler("/js/**")
                    .addResourceLocations("classpath:/static/js/")
                    .setCachePeriod(3600);

            registry.addResourceHandler("/images/**")
                    .addResourceLocations("classpath:/static/images/")
                    .setCachePeriod(3600);

            registry.addResourceHandler("/icons/**")
                    .addResourceLocations("classpath:/static/icons/")
                    .setCachePeriod(3600);
            // ===== END ADDED LINES =====

            // Log configuration
            log.info("Static resources configured:");
            log.info("  - /uploads/** -> {}", resourceLocation);
            log.info("  - /css/** -> classpath:/static/css/");
            log.info("  - /js/** -> classpath:/static/js/");
            log.info("  - /images/** -> classpath:/static/images/");
            log.info("  - /icons/** -> classpath:/static/icons/");
            log.info("=== RESOURCE CONFIGURATION COMPLETE ===");

        } catch (Exception e) {
            log.error("Failed to configure static resources", e);
            throw new RuntimeException("Static resource configuration failed", e);
        }
    }

    @Bean
    public Java8TimeDialect java8TimeDialect() {
        return new Java8TimeDialect();
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    public SpringTemplateEngine templateEngine(ITemplateResolver templateResolver,
                                               Java8TimeDialect java8TimeDialect,
                                               SpringSecurityDialect springSecurityDialect) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(java8TimeDialect);
        templateEngine.addDialect(springSecurityDialect);
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver thymeleafViewResolver(SpringTemplateEngine templateEngine) {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");
        return viewResolver;
    }

}