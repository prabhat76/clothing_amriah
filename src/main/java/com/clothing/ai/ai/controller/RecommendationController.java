package com.clothing.ai.ai.controller;

import com.clothing.ai.ai.recommendation.RecommendationService;
import com.clothing.ai.catalog.dto.ProductDtos.ProductSummaryResponse;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * AI-powered product recommendation endpoints.
 */
@RestController
@RequestMapping("/ai/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI - Recommendations",
        description = "Personalised recommendations, trending products, similar items, and semantic search")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(
            summary = "Trending products",
            description = """
                    Returns the top N trending products ranked by a composite score of
                    recent views, purchases, and review activity.

                    **Public endpoint** — no authentication required.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Trending product list returned")
    })
    @SecurityRequirements
    @GetMapping("/trending")
    public ApiResponse<List<ProductSummaryResponse>> trending(
            @Parameter(description = "Max results (1-50)") @RequestParam(defaultValue = "12") int limit) {
        return ApiResponse.success(recommendationService.trending(limit));
    }

    @Operation(
            summary = "Personalised recommendations",
            description = """
                    Returns AI-curated product recommendations tailored to the authenticated user's
                    browsing and purchase history.

                    **Falls back** to trending products for new users with no history.

                    Requires authentication.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Personalised recommendations returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/for-you")
    public ApiResponse<List<ProductSummaryResponse>> forYou(
            @Parameter(description = "Max results (1-50)") @RequestParam(defaultValue = "12") int limit) {
        return ApiResponse.success(
                recommendationService.aiPersonalized(SecurityUtils.currentUserId(), limit));
    }

    @Operation(
            summary = "Similar products",
            description = """
                    Returns products similar to the given product using vector-embedding similarity
                    (PGVector / cosine distance).

                    **Public endpoint.**
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Similar products returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Reference product not found")
    })
    @SecurityRequirements
    @GetMapping("/similar/{productId}")
    public ApiResponse<List<ProductSummaryResponse>> similar(
            @Parameter(description = "Product UUID") @PathVariable UUID productId,
            @Parameter(description = "Max results (1-30)") @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(recommendationService.similarProducts(productId, limit));
    }

    @Operation(
            summary = "Semantic product search",
            description = """
                    Natural-language semantic search over the product catalogue using OpenAI embeddings
                    + PGVector. Works better than keyword search for conceptual queries.

                    Example: `"something cosy for winter hiking"`, `"office wear for tall men"`.

                    **Public endpoint.**
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Semantic search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Empty query")
    })
    @SecurityRequirements
    @GetMapping("/search")
    public ApiResponse<List<ProductSummaryResponse>> semantic(
            @Parameter(description = "Natural language search query", required = true, example = "warm coats for winter")
            @RequestParam String q,
            @Parameter(description = "Max results") @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(recommendationService.semanticSearch(q, limit));
    }
}
