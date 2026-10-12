package com.example.recruitment.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 公开图片映射：avatar/logo/image 通过 /api/files/{category}/** 访问；简历附件由受保护 Controller 提供
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (String category : new String[]{"avatar", "logo", "image"}) {
            String location = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(category).toUri().toString();
            registry.addResourceHandler("/api/files/" + category + "/**")
                    .addResourceLocations(location.endsWith("/") ? location : location + "/");
        }
    }
}
