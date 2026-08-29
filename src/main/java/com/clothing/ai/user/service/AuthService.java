package com.clothing.ai.user.service;

import com.clothing.ai.common.exception.*;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.security.JwtService;
import com.clothing.ai.user.dto.AuthDtos.*;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties props;

    private final Map<String, UUID> passwordResetTokens = new ConcurrentHashMap<>();

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email()))
            throw new ConflictException("Email already registered");
        User user = User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .phone(req.phone())
                .role(User.Role.CUSTOMER)
                .provider(User.AuthProvider.LOCAL)
                .enabled(true)          // Lombok @Builder ignores field defaults — must set explicitly
                .emailVerified(false)
                .build();
        user = userRepository.save(user);
        return tokensFor(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!user.isEnabled()) throw new ForbiddenException("Account disabled");
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash()))
            throw new UnauthorizedException("Invalid credentials");
        user.setLastLoginAt(Instant.now());
        return tokensFor(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        var claims = jwtService.parse(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) throw new UnauthorizedException("Invalid refresh token");
        User user = userRepository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return tokensFor(user);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            String token = UUID.randomUUID().toString();
            passwordResetTokens.put(token, u.getId());
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        UUID id = passwordResetTokens.remove(token);
        if (id == null) throw new BadRequestException("Invalid or expired reset token");
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        u.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User u = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","id",userId));
        if (u.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, u.getPasswordHash()))
            throw new BadRequestException("Current password incorrect");
        u.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private TokenResponse tokensFor(User u) {
        String access = jwtService.generateAccessToken(u.getId(), u.getEmail(), u.getRole().name());
        String refresh = jwtService.generateRefreshToken(u.getId());
        return new TokenResponse(access, refresh, props.getJwt().getAccessTokenExpiration(), toUserResponse(u));
    }

    public UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getPhone(),
                u.getAvatarUrl(), u.getRole().name(), u.getHeightCm(), u.getWeightKg(),
                u.getGender() != null ? u.getGender().name() : null);
    }
}
