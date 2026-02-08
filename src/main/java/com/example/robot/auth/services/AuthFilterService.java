package com.example.robot.auth.services;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// @Service marchează clasa ca fiind un serviciu Spring.
// OncePerRequestFilter asigură că filtrul este executat o singură dată per request.
@Service
public class AuthFilterService extends OncePerRequestFilter {

    // Serviciul pentru manipularea JWT-urilor.
    private final JwtService jwtService;

    // Serviciul pentru încărcarea detaliilor utilizatorului.
    private final UserDetailsService userDetailsService;

    // Constructor pentru injectarea dependențelor.
    public AuthFilterService(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }


    // Metoda principală a filtrului, executată pentru fiecare request.
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Obține header-ul de autorizare din request.
        final String authHeader = request.getHeader("Authorization");
        String jwt;
        String username;

        // Verifică dacă header-ul există și începe cu "Bearer ".
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Dacă nu, continuă cu următorul filtru din lanț fără a face nimic.
            filterChain.doFilter(request, response);
            return;
        }

        // Extrage token-ul JWT din header (elimină prefixul "Bearer ").
        jwt = authHeader.substring(7);

        // Extrage username-ul din token.
        username = jwtService.extractUsername(jwt);

        // Dacă username-ul există și utilizatorul nu este deja autentificat în contextul de securitate.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Încarcă detaliile utilizatorului din baza de date.
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Verifică dacă token-ul este valid pentru utilizatorul respectiv.
            if(jwtService.isTokenValid(jwt, userDetails)) {
                // Creează un obiect de autentificare.
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Setează detaliile autentificării (ex: adresa IP).
                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Setează autentificarea în contextul de securitate Spring.
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        // Continuă cu următorul filtru din lanț.
        filterChain.doFilter(request, response);
    }
}
