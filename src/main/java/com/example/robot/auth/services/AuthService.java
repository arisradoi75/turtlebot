package com.example.robot.auth.services;

import com.example.robot.auth.entities.User;
import com.example.robot.auth.entities.TypeUser;
import com.example.robot.auth.repositories.UserRepository;
import com.example.robot.auth.utils.AuthResponse;
import com.example.robot.auth.utils.LoginRequest;
import com.example.robot.auth.utils.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


// @Service indică faptul că această clasă este un serviciu Spring (componentă de business logic).
@Service
// @RequiredArgsConstructor generează un constructor cu argumente pentru toate câmpurile finale (final).
@RequiredArgsConstructor
public class AuthService {

    // Encoder pentru criptarea parolelor.
    private final PasswordEncoder passwordEncoder;
    // Repository pentru accesul la datele utilizatorilor.
    private final UserRepository userRepository;
    // Serviciu pentru generarea token-urilor JWT.
    private final JwtService jwtService;
    // Serviciu pentru gestionarea refresh token-urilor.
    private final RefreshTokenService refreshTokenService;
    // Managerul de autentificare Spring Security.
    private final AuthenticationManager authenticationManager;

    // Metoda pentru înregistrarea unui utilizator nou.
    public AuthResponse register(RegisterRequest registerRequest) {
        // Construiește un obiect User folosind datele din cererea de înregistrare.
        var user = User.builder()
                .name(registerRequest.getName()) // Setează numele.
                .email(registerRequest.getEmail()) // Setează email-ul.
                .username(registerRequest.getUsername()) // Setează username-ul.
                .password(passwordEncoder.encode(registerRequest.getPassword())) // Criptează și setează parola.
                .type(TypeUser.USER) // Setează tipul utilizatorului (implicit USER).
                .build();

        // Salvează utilizatorul în baza de date.
        User savedUser = userRepository.save(user);

        // Generează un access token JWT pentru utilizatorul salvat.
        var accessToken = jwtService.generateToken(savedUser);

        // Creează un refresh token pentru utilizator.
        var refreshToken = refreshTokenService.createRefreshToken(savedUser.getEmail());

        // Returnează un obiect AuthResponse care conține token-urile și detaliile utilizatorului.
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .build();
    }

    // Metoda pentru autentificarea unui utilizator.
    public AuthResponse login(LoginRequest loginRequest) {
        // Autentifică utilizatorul folosind AuthenticationManager.
        // Dacă autentificarea eșuează, se va arunca o excepție.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), // Folosește email-ul ca principal.
                        loginRequest.getPassword() // Parola.
                )
        );

        // Caută utilizatorul în baza de date după email.
        var user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found!")); // Aruncă excepție dacă nu e găsit.

        // Generează un access token JWT.
        var accessToken = jwtService.generateToken(user);

        // Creează (sau returnează unul existent) un refresh token.
        var refreshToken = refreshTokenService.createRefreshToken(loginRequest.getEmail());

        // Returnează răspunsul cu token-uri.
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getRefreshToken())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
