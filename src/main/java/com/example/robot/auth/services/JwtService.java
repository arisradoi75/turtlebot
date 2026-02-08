package com.example.robot.auth.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// @Service marchează clasa ca fiind un serviciu Spring.
@Service
public class JwtService {
    // Cheia secretă folosită pentru semnarea token-urilor JWT. Ar trebui să fie păstrată în siguranță (ex: variabile de mediu).
    private static final String SECRET_KEY = "BF7FD11ACE545745B7BA1AF98B6F156D127BC7BB544BAB6A4FD74E4FC7";

    // Extrage username-ul (subiectul) din token-ul JWT.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Metodă generică pentru extragerea unei anumite informații (claim) din token.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extrage toate informațiile (claims) din token-ul JWT.
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey()) // Setează cheia de semnare pentru verificare.
                .build()
                .parseClaimsJws(token) // Parsează token-ul.
                .getBody(); // Returnează corpul token-ului (payload).
    }

    // Decodifică cheia secretă din format Base64 și returnează un obiect Key.
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generează un token JWT pentru un utilizator, fără claim-uri extra.
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    // Generează un token JWT cu claim-uri extra și detaliile utilizatorului.
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        extraClaims = new HashMap<>(extraClaims);
        // Adaugă rolurile utilizatorului în claim-uri.
        extraClaims.put("role", userDetails.getAuthorities());
        return Jwts
                .builder()
                .setClaims(extraClaims) // Setează claim-urile.
                .setSubject(userDetails.getUsername()) // Setează subiectul (username-ul).
                .setIssuedAt(new Date(System.currentTimeMillis())) // Setează data emiterii.
                .setExpiration(new Date(System.currentTimeMillis() + 25 * 100000)) // Setează data expirării.
                .signWith(getSignInKey(), SignatureAlgorithm.HS256) // Semnează token-ul cu cheia secretă și algoritmul HS256.
                .compact(); // Construiește token-ul string.
    }

    // Verifică dacă un token este valid pentru un anumit utilizator.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        // Token-ul este valid dacă username-ul corespunde și token-ul nu este expirat.
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Verifică dacă token-ul este expirat.
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extrage data expirării din token.
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
