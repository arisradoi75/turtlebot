package com.example.robot.auth.config;

import com.example.robot.auth.services.AuthFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final AuthFilterService authFilterService;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(req -> req
                        // API-uri Publice (autentificare, handshake WebSocket și primire date de la robot)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/ws-robot/**").permitAll()
                        .requestMatchers("/api/robot/telemetry").permitAll() // Folosit de scriptul Python
                        .requestMatchers("/api/robot/alert").permitAll()    // Folosit de scriptul Python

                        // API-uri de Admin - NECESITĂ ROL DE ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // Orice altceva (inclusiv /api/robot/latest-telemetry) necesită doar Autentificare
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(authFilterService, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
