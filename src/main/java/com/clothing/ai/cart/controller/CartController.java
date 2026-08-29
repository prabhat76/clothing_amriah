package com.clothing.ai.cart.controller;

import com.clothing.ai.cart.dto.CartDtos.*;
import com.clothing.ai.cart.service.CartService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Shopping cart endpoints.
 *
 * <p>The cart is created automatically on the first add-item call.
 * All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart: add, update quantity, remove items, clear")
@SecurityRequirement(name = "BearerAuth")
public class CartController {

    private final CartService cartService;

    @Operation(
            summary = "Get current cart",
            description = """
                    Returns the authenticated user's active cart with all line items.
                    Creates an empty cart if one does not exist yet.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Cart returned",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @GetMapping
    public ApiResponse<CartResponse> get() {
        return ApiResponse.success(cartService.getOrCreateCart(SecurityUtils.currentUserId()));
    }

    @Operation(
            summary = "Add item to cart",
            description = """
                    Adds a product variant to the cart.
                    If the variant is already in the cart its quantity is **incremented** by the supplied amount.

                    **Edge cases:**
                    - Quantity > stock → `400 INSUFFICIENT_STOCK`
                    - Variant not found → `404`
                    - Variant inactive/out-of-stock → `400`
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Item added, updated cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Insufficient stock or inactive variant"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Variant not found")
    })
    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody AddToCartRequest req) {
        return ApiResponse.success("Item added", cartService.addItem(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "Update cart item quantity",
            description = """
                    Sets the quantity of a specific cart line item.
                    Use quantity `0` to remove the item (or call `DELETE /cart/items/{itemId}`).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Quantity updated, updated cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid quantity or insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Cart item not found or does not belong to this user")
    })
    @PatchMapping("/items/{itemId}")
    public ApiResponse<CartResponse> updateItem(
            @Parameter(description = "Cart item UUID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemRequest req) {
        return ApiResponse.success("Item updated",
                cartService.updateItem(SecurityUtils.currentUserId(), itemId, req));
    }

    @Operation(
            summary = "Remove an item from cart",
            description = "Removes the specified line item. Returns the updated cart.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Item removed, updated cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Cart item not found")
    })
    @DeleteMapping("/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(
            @Parameter(description = "Cart item UUID") @PathVariable UUID itemId) {
        return ApiResponse.success("Item removed",
                cartService.removeItem(SecurityUtils.currentUserId(), itemId));
    }

    @Operation(
            summary = "Clear the entire cart",
            description = "Removes all items from the cart. The cart record itself is preserved.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Cart cleared")
    })
    @DeleteMapping
    public ApiResponse<Void> clear() {
        cartService.clearCart(SecurityUtils.currentUserId());
        return ApiResponse.success("Cart cleared", null);
    }
}
