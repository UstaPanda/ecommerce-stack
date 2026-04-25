package com.ecommerce.main.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * /uploads/** URL'lerini disk üzerindeki klasöre yönlendirir.
 * Tarayıcı http://localhost:8080/uploads/products/abc.jpg istediğinde
 * Spring diskten okuyup döner.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${storage.local.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
