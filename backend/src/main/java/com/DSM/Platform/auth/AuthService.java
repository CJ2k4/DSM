package com.DSM.Platform.auth;

import com.DSM.Platform.auth.RefreshTokenService.IssuedRefreshToken;
import com.DSM.Platform.auth.dto.AuthResponse;
import com.DSM.Platform.auth.dto.LoginRequest;
import com.DSM.Platform.auth.dto.RegisterRequest;
import com.DSM.Platform.auth.dto.UserResponse;
import com.DSM.Platform.common.exception.ApiException;
import com.DSM.Platform.security.AuthenticatedUser;
import com.DSM.Platform.security.JwtService;
import com.DSM.Platform.user.User;
import com.DSM.Platform.user.UserRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username is already taken");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        String displayName = StringUtils.hasText(request.displayName()) ? request.displayName().trim() : username;
        User user = new User(username, email, passwordEncoder.encode(request.password()), displayName);
        User savedUser = userRepository.save(user);

        return issueAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = normalizeIdentifier(request.identifier());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, request.password()));

        User user = userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials"));

        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenService.validateToken(rawRefreshToken);
        refreshTokenService.revoke(refreshToken);

        return issueAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeByRawToken(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthenticatedUser principal) {
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return UserResponse.from(user);
    }

    private AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        IssuedRefreshToken refreshToken = refreshTokenService.issueToken(user);
        return AuthResponse.bearer(
                accessToken,
                refreshToken.token(),
                jwtService.accessTokenTtlSeconds(),
                UserResponse.from(user)
        );
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.trim().toLowerCase(Locale.ROOT);
    }
}
