package com.clothing.ai.catalog.dto;

import com.clothing.ai.catalog.entity.Product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Catalogue module request and response DTOs.
 */
public class ProductDtos {

    // ------------------------------------------------------------------ categories

    @Schema(description = "Category create / update request")
    public record CategoryRequest(
            @Schema(description = "Category display name", example = "Men's Outerwear", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 120) String name,
            @Schema(description = "Optional description")
            @Size(max = 1000) String description,
            @Schema(description = "Category banner image URL")
            String imageUrl,
            @Schema(description = "UUID of parent category for sub-category nesting")
            UUID parentId,
            @Schema(description = "Display order (ascending)", example = "10")
            Integer displayOrder) {}

    @Schema(description = "Category summary")
    public record CategoryResponse(UUID id, String name, String slug, String description,
                                   String imageUrl, UUID parentId, Integer displayOrder, boolean active) {}

    // ------------------------------------------------------------------ brands

    @Schema(description = "Brand create / update request")
    public record BrandRequest(
            @NotBlank @Size(max = 120) String name,
            String description,
            String logoUrl) {}

    @Schema(description = "Brand summary")
    public record BrandResponse(UUID id, String name, String slug, String description, String logoUrl, boolean active) {}

    // ------------------------------------------------------------------ products

    @Schema(description = "Create a new product")
    public record ProductCreateRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Classic White T-Shirt")
            @NotBlank @Size(max = 200) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "SKU-CWT-001")
            @NotBlank @Size(max = 60) String sku,
            String description,
            @Schema(description = "Short teaser shown in list views")
            String shortDescription,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID categoryId,
            UUID brandId,
            @Schema(example = "29.99", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.0") BigDecimal price,
            @Schema(description = "Original price shown as strikethrough", example = "39.99")
            BigDecimal compareAtPrice,
            @Schema(description = "Internal cost price (not exposed to customers)")
            BigDecimal costPrice,
            @Schema(example = "USD")
            String currency,
            String mainImageUrl,
            Set<String> imageUrls,
            Set<String> tags,
            boolean featured,
            boolean newArrival,
            Integer weightGrams,
            @Schema(description = "Initial variants — can be empty, add later via POST /products/{id}/variants")
            List<VariantCreateRequest> variants) {}

    @Schema(description = "Partial product update — null fields are ignored")
    public record ProductUpdateRequest(
            String name,
            String description,
            String shortDescription,
            UUID categoryId,
            UUID brandId,
            BigDecimal price,
            BigDecimal compareAtPrice,
            BigDecimal costPrice,
            String mainImageUrl,
            Set<String> imageUrls,
            Set<String> tags,
            Boolean featured,
            Boolean newArrival,
            Integer weightGrams,
            Boolean active,
            @Schema(description = "Product lifecycle status",
                    allowableValues = {"DRAFT", "ACTIVE", "OUT_OF_STOCK", "DISCONTINUED", "ARCHIVED"})
            ProductStatus status) {}

    // ------------------------------------------------------------------ variants

    @Schema(description = "Add a size/colour/material variant to a product")
    public record VariantCreateRequest(
            @NotBlank @Size(max = 60) String sku,
            @Schema(description = "Size label", example = "M") String size,
            @Schema(description = "Colour name", example = "Navy Blue") String color,
            @Schema(description = "Hex colour code", example = "#1a237e") String colorHex,
            String material,
            @NotNull @DecimalMin("0.0") BigDecimal price,
            BigDecimal salePrice,
            @Schema(minimum = "0") int stockQuantity,
            @Schema(description = "Low-stock warning threshold", example = "5") Integer lowStockThreshold,
            String imageUrl,
            String barcode) {}

    @Schema(description = "Variant detail")
    public record VariantResponse(
            UUID id, UUID productId, String sku, String size, String color, String colorHex,
            String material, BigDecimal price, BigDecimal salePrice, BigDecimal effectivePrice,
            int stockQuantity, String imageUrl, String barcode, boolean active) {}

    // ------------------------------------------------------------------ product responses

    @Schema(description = "Lightweight product card for list / search views")
    public record ProductSummaryResponse(
            UUID id, String name, String slug, String sku, String shortDescription,
            String mainImageUrl, BigDecimal price, BigDecimal compareAtPrice,
            BigDecimal averageRating, int reviewCount, boolean featured, boolean newArrival,
            UUID categoryId, String categoryName, UUID brandId, String brandName,
            BigDecimal minVariantPrice) {}

    @Schema(description = "Full product detail including variants, images, and tags")
    public record ProductDetailResponse(
            ProductSummaryResponse product,
            String description,
            String aiGeneratedDescription,
            Set<String> imageUrls,
            Set<String> tags,
            List<VariantResponse> variants,
            List<String> availableSizes,
            List<String> availableColors,
            long viewCount,
            Instant createdAt,
            Instant updatedAt) {}
}
