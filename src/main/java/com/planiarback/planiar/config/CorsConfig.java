package com.planiarback.planiar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        java.lang.String originsEnv = System.getenv("FRONTEND_ORIGINS");
        final String[] allowedOrigins;
        if (originsEnv != null && !originsEnv.isBlank()) {
            allowedOrigins = originsEnv.split(",");
        } else {
            allowedOrigins = new String[] { "http://localhost:3000" };
        }

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("*")
                        .allowCredentials(true);
            }
        };
    }
}