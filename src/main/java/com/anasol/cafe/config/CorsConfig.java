package com.anasol.cafe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:}") // Default to  if missing
    private String allowedOrigins;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Allow Credentials (cookies/auth headers)
        config.setAllowCredentials(true);

        // 2. Smart Origin Handling
        // Splits by comma and TRIMS spaces (common mistake in .env files)
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        // Use Patterns to allow "*" with credentials
        config.setAllowedOriginPatterns(origins);

        // 3. Allow All Headers & Methods
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // 4. Explicitly allow SSE specific headers just in case
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("Last-Event-ID");

        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);

        // 5. Create Filter with HIGHEST Precedence
        // This ensures CORS headers are added BEFORE Spring Security runs
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}