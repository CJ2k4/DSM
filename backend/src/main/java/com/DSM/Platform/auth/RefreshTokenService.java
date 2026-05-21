package com.DSM.Platform.auth;

import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.config.JwtProperties;
import com.DSM.Platform.user.User;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public IssuedRefreshToken issueToken(User user) {
        String rawToken = generateToken();
        Instant expiresAt = Instant.now().plus(jwtProperties.getRefreshTokenExpiration());

        refreshTokenRepository.save(new RefreshToken(user, hashToken(rawToken), expiresAt));

        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> invalidRefreshToken());

        if (!refreshToken.isActive(Instant.now())) {
            throw invalidRefreshToken();
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        refreshToken.revoke(Instant.now());
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(this::revoke);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hashing is not available", exception);
        }
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String token, Instant expiresAt) {
    }
}
