package com.example.robot.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Soluția robustă: Permitem orice port de pe localhost.
        configuration.setAllowedOriginPatterns(Collections.singletonList("http://localhost:*"));
        
        // Permitem toate metodele standard (GET, POST, PUT, DELETE, etc.)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Permitem toate headerele, inclusiv "Authorization" pentru JWT și "Content-Type".
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Permitem trimiterea de credențiale (ex: cookies), deși nu este cazul acum, e o practică bună.
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplicăm această configurație pentru TOATE rutele din aplicație.
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
