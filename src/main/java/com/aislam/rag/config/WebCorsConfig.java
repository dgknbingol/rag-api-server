package com.aislam.rag.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    public WebCorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);

        if (corsProperties.allowedOriginPatterns() != null && !corsProperties.allowedOriginPatterns().isEmpty()) {
            registration.allowedOriginPatterns(corsProperties.allowedOriginPatterns().toArray(String[]::new));
        } else {
            registration.allowedOriginPatterns("*");
        }
    }
}
