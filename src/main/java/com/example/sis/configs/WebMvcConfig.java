package com.example.sis.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Config để serve static files (uploaded files)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir:uploads/syllabus}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Lấy đường dẫn tuyệt đối của thư mục upload
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString();
        
        // Map URL /uploads/syllabus/** to file system
        registry.addResourceHandler("/uploads/syllabus/**")
                .addResourceLocations("file:" + absolutePath + "/")
                .setCachePeriod(3600); // Cache 1 giờ
        
        System.out.println("📁 Static files location: file:" + absolutePath + "/");
    }
}
