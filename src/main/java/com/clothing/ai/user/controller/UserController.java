package com.clothing.ai.user.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import com.clothing.ai.user.dto.AddressDtos.*;
import com.clothing.ai.user.dto.AuthDtos.*;
import com.clothing.ai.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Authenticated user profile and address-book endpoints.
 *
 * <p>All endpoints require a valid JWT Bearer token.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile, address book and account management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    // ------------------------------------------------------------------ profile

    @Operation(
            summary = "Get current user's profile",
            description = "Returns the authenticated user's profile including account details.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.success(
                userService.authService().toUserResponse(
                        userService.getOrThrow(SecurityUtils.currentUserId())));
    }

    @Operation(
            summary = "Update current user's profile",
            description = """
                    Partial update — only fields present in the request body are updated.
                    You cannot change `email` or `role` via this endpoint.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(@RequestBody UserResponse req) {
        return ApiResponse.success("Profile updated",
                userService.updateProfile(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "Change password",
            description = """
                    Requires the current password for verification before accepting the new one.
                    All existing refresh tokens are invalidated after a successful password change.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Password changed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "New password does not meet policy or current password wrong"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest req) {
        userService.getAuthService()
                .changePassword(SecurityUtils.currentUserId(), req.currentPassword(), req.newPassword());
        return ApiResponse.success("Password changed successfully", null);
    }

    // ------------------------------------------------------------------ addresses

    @Operation(
            summary = "List saved addresses",
            description = "Returns all shipping/billing addresses for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address list returned")
    })
    @GetMapping("/me/addresses")
    public ApiResponse<List<AddressResponse>> listAddresses() {
        return ApiResponse.success(userService.listAddresses(SecurityUtils.currentUserId()));
    }

    @Operation(
            summary = "Create a new address",
            description = "Adds a new shipping/billing address to the user's address book.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Address created",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error — missing required fields")
    })
    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AddressResponse> createAddress(@Valid @RequestBody AddressRequest req) {
        return ApiResponse.success("Address created",
                userService.createAddress(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "Update an existing address",
            description = "Full replacement of the address identified by `{id}`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Address not found")
    })
    @PutMapping("/me/addresses/{id}")
    public ApiResponse<AddressResponse> updateAddress(
            @Parameter(description = "Address UUID") @PathVariable UUID id,
            @Valid @RequestBody AddressRequest req) {
        return ApiResponse.success("Address updated",
                userService.updateAddress(SecurityUtils.currentUserId(), id, req));
    }

    @Operation(
            summary = "Delete an address",
            description = "Permanently removes an address from the address book.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Address deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Address not found or not owned by this user")
    })
    @DeleteMapping("/me/addresses/{id}")
    public ApiResponse<Void> deleteAddress(
            @Parameter(description = "Address UUID") @PathVariable UUID id) {
        userService.deleteAddress(SecurityUtils.currentUserId(), id);
        return ApiResponse.success("Address deleted", null);
    }
}
