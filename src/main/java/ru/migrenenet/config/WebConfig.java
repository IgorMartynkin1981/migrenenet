package ru.migrenenet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** CORS нужен, если интерфейс открыт с другого адреса (например, GitHub Pages). */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors-origin:}") private String origin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origin.isBlank()) return;
        registry.addMapping("/api/**").allowedOrigins(origin).allowedMethods("GET", "POST").allowedHeaders("*");
    }
}
