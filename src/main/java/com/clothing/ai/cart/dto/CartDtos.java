package com.clothing.ai.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CartDtos {
    public record AddToCartRequest(@NotNull UUID variantId, @Min(1) int quantity) {}
    public record UpdateCartItemRequest(@Min(1) int quantity) {}

    public record CartItemResponse(UUID id, UUID variantId, UUID productId, String productName,
                                    String size, String color, String imageUrl, String sku,
                                    BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}

    public record CartResponse(UUID id, List<CartItemResponse> items,
                                BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                                BigDecimal tax, BigDecimal total, int itemCount) {}

    public record WishlistResponse(UUID id, UUID productId, String productName, String slug,
                                    String imageUrl, BigDecimal price, BigDecimal averageRating) {}
}
