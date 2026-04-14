package com.smartims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 映射本地目录为 HTTP 静态资源，与 {@link com.smartims.service.ProductImageStorageService} 存盘路径一致。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.attachment-dir:data}")
    private String attachmentDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(attachmentDir, "uploads").toAbsolutePath().normalize();
        String location = uploadRoot.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
