package com.example.robot.auth.services;
import com.example.robot.auth.entities.RefreshToken;
import com.example.robot.auth.entities.User;
import com.example.robot.auth.repositories.RefreshTokenRepository;
import com.example.robot.auth.repositories.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;


// @Service marchează clasa ca fiind un serviciu Spring.
@Service
public class RefreshTokenService {

    // Repository pentru accesul la datele utilizatorilor.
    private final UserRepository userRepository;

    // Repository pentru accesul la datele refresh token-urilor.
    private final RefreshTokenRepository refreshTokenRepository;

    // Constructor pentru injectarea dependențelor.
    public RefreshTokenService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Creează un nou refresh token pentru un utilizator.
    public RefreshToken createRefreshToken(String username) {
        // Caută utilizatorul după email (care este folosit ca username).
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email : " + username));

        // Verifică dacă utilizatorul are deja un refresh token asociat.
        RefreshToken refreshToken = user.getRefreshToken();

        // Dacă nu există un refresh token, se creează unul nou.
        if (refreshToken == null) {
            // Setează durata de valabilitate a token-ului.
            long refreshTokenValidity = 30 * 100000;
            // Construiește un nou obiect RefreshToken.
            refreshToken = RefreshToken.builder()
                    .refreshToken(UUID.randomUUID().toString()) // Generează un token unic.
                    .expirationTime(Instant.now().plusMillis(refreshTokenValidity)) // Setează timpul de expirare.
                    .user(user) // Asociază token-ul cu utilizatorul.
                    .build();

            // Salvează noul token în baza de date.
            refreshTokenRepository.save(refreshToken);
        }

        // Returnează token-ul (fie cel nou creat, fie cel existent).
        return refreshToken;
    }

    // Verifică validitatea unui refresh token.
    public RefreshToken verifyRefreshToken(String refreshToken) {
        // Caută token-ul în baza de date după valoarea sa.
        RefreshToken refToken = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found!"));

        // Verifică dacă token-ul a expirat.
        if (refToken.getExpirationTime().compareTo(Instant.now()) < 0) {
            // Dacă a expirat, îl șterge din baza de date.
            refreshTokenRepository.delete(refToken);
            // Aruncă o excepție pentru a informa clientul.
            throw new RuntimeException("Refresh Token expired");
        }

        // Dacă token-ul este valid, îl returnează.
        return refToken;
    }
}