package com.example.robot.auth.config;

import com.example.robot.auth.repositories.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// @Configuration indică faptul că această clasă conține definiții de bean-uri Spring.
@Configuration
public class ApplicationConfig {

    private final UserRepository userRepository;

    // Constructor pentru injectarea repository-ului de utilizatori.
    public ApplicationConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Definește un bean pentru UserDetailsService.
    // Acesta este folosit de Spring Security pentru a încărca datele utilizatorului în timpul autentificării.
    @Bean
    public UserDetailsService userDetailsService() {
        // Implementare funcțională: caută utilizatorul după email.
        // Dacă nu este găsit, aruncă o excepție UsernameNotFoundException.
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    // Definește un bean pentru AuthenticationProvider.
    // Acesta este responsabil pentru logica de autentificare (verificarea credențialelor).
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Folosim DaoAuthenticationProvider, care este implementarea standard ce folosește un UserDetailsService și un PasswordEncoder.
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService()); // Setează serviciul de încărcare a utilizatorilor.
        authenticationProvider.setPasswordEncoder(passwordEncoder()); // Setează encoder-ul pentru verificarea parolelor.
        return authenticationProvider;
    }

    // Definește un bean pentru AuthenticationManager.
    // Acesta este punctul central de intrare pentru autentificare în Spring Security.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Obține AuthenticationManager din configurația Spring Security.
        return config.getAuthenticationManager();
    }

    // Definește un bean pentru PasswordEncoder.
    // Acesta este folosit pentru a cripta parolele la înregistrare și pentru a le verifica la login.
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Folosim BCrypt, un algoritm de hashing puternic și standard în industrie.
        return new BCryptPasswordEncoder();
    }

}
