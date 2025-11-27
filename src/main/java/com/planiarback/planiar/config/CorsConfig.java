package com.planiarback.planiar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String originsEnv = System.getenv("FRONTEND_ORIGINS");
        System.out.println("DEBUG: FRONTEND_ORIGINS env = [" + originsEnv + "]");
        final String[] allowedOrigins;
        if (originsEnv != null && !originsEnv.isBlank()) {
            allowedOrigins = Arrays.stream(originsEnv.split(","))
                    .map(String::trim)
                    .map(s -> {
                        if (s.isEmpty()) return s;
                        // si no tiene protocolo, asumir https
                        if (!s.matches("(?i)^https?://.*")) {
                            return "https://" + s;
                        }
                        return s;
                    })
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
        } else {
            allowedOrigins = new String[] { "http://localhost:3000" };
        }

        System.out.println("DEBUG: allowedOrigins = " + Arrays.toString(allowedOrigins));

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