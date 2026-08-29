package com.clothing.ai.review.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.review.dto.ReviewDtos.*;
import com.clothing.ai.review.service.ReviewService;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Product review and rating endpoints.
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product reviews and ratings: create, list, helpful votes, admin delete")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "List reviews for a product",
            description = """
                    Returns paginated reviews for the given product, sorted by `helpfulCount DESC` then `createdAt DESC`.

                    **Visible to:** everyone (public endpoint).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Review page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @SecurityRequirements
    @GetMapping("/product/{productId}")
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @Parameter(description = "Product UUID", in = ParameterIn.PATH) @PathVariable UUID productId,
            @Parameter(description = "Rating filter (1-5)", in = ParameterIn.QUERY) @RequestParam(required = false) Integer rating,
            @Parameter(description = "Only verified-purchase reviews") @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reviewService.listForProduct(productId, page, size));
    }

    @Operation(
            summary = "Submit a product review",
            description = """
                    Creates a review for a product.

                    **Rules:**
                    - One review per customer per product.
                    - `verifiedPurchase` is set automatically if the customer has a delivered order containing this product.
                    - `sizeFit` and `quality` are optional sub-ratings (1-5).
                    - Submitting automatically recalculates `averageRating` and `reviewCount` on the product.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Review created",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Review already exists for this product by this user")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest req) {
        return ApiResponse.success("Review submitted",
                reviewService.create(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "Mark a review as helpful",
            description = "Increments the helpful counter for a review. Idempotent per session.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Helpful count incremented"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{id}/helpful")
    public ApiResponse<Void> helpful(
            @Parameter(description = "Review UUID") @PathVariable UUID id) {
        reviewService.helpful(id);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "Delete a review [ADMIN]",
            description = """
                    Permanently removes a review and recalculates product rating stats.

                    Requires role `ADMIN`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Review deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Review not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(
            @Parameter(description = "Review UUID") @PathVariable UUID id) {
        reviewService.delete(id);
        return ApiResponse.success("Review deleted", null);
    }
}
