package com.DSM.Platform.security;

import com.DSM.Platform.config.JwtProperties;
import com.DSM.Platform.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = createSigningKey(jwtProperties.getSecret());
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpiration());

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public boolean isTokenValid(String token, AuthenticatedUser user) {
        try {
            UUID tokenUserId = extractUserId(token);
            return tokenUserId.equals(user.id()) && parseClaims(token).getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public long accessTokenTtlSeconds() {
        return jwtProperties.getAccessTokenExpiration().toSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key createSigningKey(String secret) {
        byte[] keyBytes = decodeBase64IfPossible(secret);
        if (keyBytes.length < 32) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decodeBase64IfPossible(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
