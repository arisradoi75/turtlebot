package com.example.robot.controller;

import com.example.robot.auth.entities.RefreshToken;
import com.example.robot.auth.entities.User;
import com.example.robot.auth.services.AuthService;
import com.example.robot.auth.services.JwtService;
import com.example.robot.auth.services.RefreshTokenService;
import com.example.robot.auth.utils.AuthResponse;
import com.example.robot.auth.utils.LoginRequest;
import com.example.robot.auth.utils.RefreshTokenRequest;
import com.example.robot.auth.utils.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// @RestController marchează această clasă ca fiind un controller Spring MVC unde fiecare metodă returnează un obiect de domeniu în loc de o vizualizare.
@RestController
// @RequestMapping definește prefixul URL pentru toate endpoint-urile din acest controller.
@RequestMapping("/api/v1/auth")
public class AuthController {

    // Serviciul principal pentru logica de autentificare și înregistrare.
    private final AuthService authService;
    // Serviciul pentru gestionarea token-urilor de refresh.
    private final RefreshTokenService refreshTokenService;
    // Serviciul pentru operațiuni legate de JWT (generare, validare).
    private final JwtService jwtService;

    // Constructorul pentru injectarea dependențelor (Dependency Injection).
    public AuthController(AuthService authService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    // Endpoint pentru înregistrarea unui utilizator nou.
    // @PostMapping mapează cererile HTTP POST la această metodă.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        // Apelează serviciul de autentificare pentru a înregistra utilizatorul și returnează răspunsul (inclusiv token-uri) într-un ResponseEntity cu status 200 OK.
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    // Endpoint pentru autentificarea unui utilizator existent.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        // Apelează serviciul de autentificare pentru a loga utilizatorul și returnează răspunsul.
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    // Endpoint pentru reîmprospătarea token-ului de acces folosind un refresh token.
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        // Verifică dacă refresh token-ul primit este valid (există în DB și nu a expirat).
        RefreshToken refresgToken = refreshTokenService.verifyRefreshToken(refreshTokenRequest.getRefreshToken());

        // Obține utilizatorul asociat acestui refresh token.
        User user = refresgToken.getUser();

        // Generează un nou access token pentru utilizator.
        String accesToken = jwtService.generateToken(user);

        // Returnează noul access token și refresh token-ul existent într-un obiect AuthResponse.
        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accesToken)
                .refreshToken(refresgToken.getRefreshToken())
                .build());
    }

}
