package com.minidrive.minigoogledrive.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET =
            "ThisIsMyVerySecureSecretKeyForMiniGoogleDriveProject123456789";


    private final SecretKey key = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)
    );


    // Generate JWT Token
    public String generateToken(String username) {

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)
                )
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    // Extract username
    public String extractUsername(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }


    // Validate token
    public boolean validateToken(String token, String username) {

        return extractUsername(token).equals(username)
                && !isTokenExpired(token);
    }


    private boolean isTokenExpired(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getExpiration().before(new Date());
    }


    // Used in JwtFilter
    public SecretKey getSigningKey() {
        return key;
    }
}