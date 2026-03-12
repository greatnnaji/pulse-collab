package com.pulse.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(Long userId, String username) {
        // TODO: Implement JWT token generation
        // 1. Create claims with userId and username
        // 2. Set expiration time
        // 3. Sign with secret key
        // 4. Return token string
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // TODO: Build and return JWT token
        // Use Jwts.builder() with claims, subject, issued date, expiration, and signature
        throw new UnsupportedOperationException("JWT token creation not implemented yet");
    }

    public String extractUsername(String token) {
        // TODO: Extract username from JWT token
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractUserId(String token) {
        // TODO: Extract userId from JWT claims
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // TODO: Extract specific claim from token
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        // TODO: Parse and extract all claims from JWT token
        // Use Jwts.parserBuilder() with signing key
        throw new UnsupportedOperationException("JWT claims extraction not implemented yet");
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        // TODO: Validate JWT token
        // 1. Extract username from token
        // 2. Check if username matches and token is not expired
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
