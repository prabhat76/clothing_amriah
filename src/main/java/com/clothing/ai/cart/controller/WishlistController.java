package com.clothing.ai.cart.controller;

import com.clothing.ai.cart.dto.CartDtos.*;
import com.clothing.ai.cart.service.WishlistService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Wishlist endpoints — save products for later.
 */
@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "Save products for later — per-user, persisted across sessions")
@SecurityRequirement(name = "BearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(
            summary = "Get my wishlist",
            description = "Returns all products the authenticated user has saved to their wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Wishlist returned",
                    content = @Content(schema = @Schema(implementation = WishlistResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ApiResponse<List<WishlistResponse>> list() {
        return ApiResponse.success(wishlistService.list(SecurityUtils.currentUserId()));
    }

    @Operation(
            summary = "Add product to wishlist",
            description = "Adds a product to the wishlist. Idempotent — adding a product already on the list is a no-op.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Product added to wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> add(
            @Parameter(description = "Product UUID to wishlist") @PathVariable UUID productId) {
        wishlistService.add(SecurityUtils.currentUserId(), productId);
        return ApiResponse.success("Added to wishlist", null);
    }

    @Operation(
            summary = "Remove product from wishlist",
            description = "Removes a product from the wishlist. Idempotent.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Product removed from wishlist"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not in wishlist")
    })
    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(
            @Parameter(description = "Product UUID to remove") @PathVariable UUID productId) {
        wishlistService.remove(SecurityUtils.currentUserId(), productId);
        return ApiResponse.success("Removed from wishlist", null);
    }

    @Operation(
            summary = "Check if product is in wishlist",
            description = "Returns `true` if the given product is on the user's wishlist.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Boolean result")
    })
    @GetMapping("/{productId}/check")
    public ApiResponse<Boolean> check(
            @Parameter(description = "Product UUID") @PathVariable UUID productId) {
        return ApiResponse.success(wishlistService.isWishlisted(SecurityUtils.currentUserId(), productId));
    }
}
