package com.clothing.ai.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Review module DTOs.
 */
public class ReviewDtos {

    @Schema(description = "Submit a product review")
    public record ReviewRequest(
            @Schema(description = "UUID of the product being reviewed", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID productId,

            @Schema(description = "Overall rating 1 (worst) – 5 (best)", minimum = "1", maximum = "5",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @Min(1) @Max(5) int rating,

            @Schema(description = "Optional review headline", example = "Great quality, runs slightly small")
            @Size(max = 150) String title,

            @Schema(description = "Full review text")
            String comment,

            @Schema(description = "Size / fit rating: 1 = runs very small, 3 = true to size, 5 = runs very large")
            @Min(1) @Max(5) Integer sizeFit,

            @Schema(description = "Fabric / build quality rating 1–5")
            @Min(1) @Max(5) Integer quality,

            @Schema(description = "OrderItem UUID to verify the purchase. Providing this enables the 'Verified Purchase' badge.")
            UUID orderItemId) {}

    @Schema(description = "Review detail response")
    public record ReviewResponse(
            UUID id,
            UUID productId,
            String productName,
            UUID userId,
            String userName,
            String userAvatar,
            int rating,
            String title,
            String comment,
            Integer sizeFit,
            Integer quality,
            boolean verifiedPurchase,
            int helpfulCount,
            Instant createdAt) {}
}
