package com.planiarback.planiar.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String originsEnv = System.getenv("FRONTEND_ORIGINS");
        System.out.println("FRONTEND_ORIGINS env = [" + originsEnv + "]");
        final String[] allowedOrigins;
        if (originsEnv != null && !originsEnv.isBlank()) {
            allowedOrigins = Arrays.stream(originsEnv.split(","))
                                .map(String::trim)
                                .toArray(String[]::new);
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