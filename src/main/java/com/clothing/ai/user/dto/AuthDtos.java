package com.clothing.ai.user.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 80) String firstName,
            @NotBlank @Size(max = 80) String lastName,
            @Size(max = 30) String phone
    ) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UserResponse(
            UUID id, String email, String firstName, String lastName, String phone,
            String avatarUrl, String role, Integer heightCm, Integer weightKg, String gender
    ) {}

    public record PasswordChangeRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 8) String newPassword) {}

    public record ForgotPasswordRequest(@NotBlank @Email String email) {}

    public record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String newPassword) {}
}
