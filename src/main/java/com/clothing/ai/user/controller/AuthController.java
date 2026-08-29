package com.clothing.ai.user.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.user.dto.AuthDtos.*;
import com.clothing.ai.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — register, login, token refresh, password management.
 *
 * <p>All endpoints here are <em>public</em> (no JWT required).
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, password reset")
@SecurityRequirements   // override global BearerAuth — these endpoints need no token
public class AuthController {

    private final AuthService authService;

    // ------------------------------------------------------------------ register

    @Operation(
            summary = "Register a new customer account",
            description = """
                    Creates a new user account with the CUSTOMER role.

                    **Password rules:** minimum 8 characters.
                    On success the response contains access + refresh tokens so the client
                    is immediately logged in without a second round-trip.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Account created and tokens issued",
                    content = @Content(schema = @Schema(implementation = TokenResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error (weak password, missing fields)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {"success":false,"errorCode":"VALIDATION_FAILED",
                                     "message":"password: size must be between 8 and 100","timestamp":"…"}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.success("Registered successfully", authService.register(req));
    }

    // ------------------------------------------------------------------ login

    @Operation(
            summary = "Log in with email and password",
            description = "Returns an access token (1 h) and a refresh token (30 days).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = TokenResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Account disabled / locked",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success("Logged in", authService.login(req));
    }

    // ------------------------------------------------------------------ refresh

    @Operation(
            summary = "Refresh access token",
            description = """
                    Exchange a valid refresh token for a new access + refresh token pair.
                    The old refresh token is invalidated (rotation pattern).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "New token pair issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Refresh token expired or invalid")
    })
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ApiResponse.success("Tokens refreshed", authService.refresh(req.refreshToken()));
    }

    // ------------------------------------------------------------------ forgot password

    @Operation(
            summary = "Request a password-reset email",
            description = """
                    Sends a password-reset link to the registered email address.

                    **Security note:** the response is always `200 OK` regardless of
                    whether the email exists — this prevents user-enumeration attacks.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Email sent (if the address is registered)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Malformed email address")
    })
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return ApiResponse.success("If the email is registered, reset instructions have been sent", null);
    }

    // ------------------------------------------------------------------ reset password

    @Operation(
            summary = "Reset password using a token from the email link",
            description = """
                    The `token` query param is the single-use UUID that was embedded in the
                    password-reset email. Tokens expire after 1 hour.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Password changed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Token expired, already used, or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "New password does not meet policy requirements")
    })
    @PostMapping("/reset-password")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
        return ApiResponse.success("Password reset successfully", null);
    }

    // ------------------------------------------------------------------ Swagger schema helpers (not real types)

    /** Only used so Swagger renders a concrete schema for ApiResponse&lt;TokenResponse&gt;. */
    @Schema(name = "TokenResponseEnvelope")
    private record TokenResponseEnvelope(boolean success, String message, TokenResponse data,
                                          String errorCode, String timestamp) {}
}
