package com.clothing.ai.catalog.mapper;

import com.clothing.ai.catalog.dto.ProductDtos.BrandResponse;
import com.clothing.ai.catalog.dto.ProductDtos.CategoryResponse;
import com.clothing.ai.catalog.dto.ProductDtos.ProductDetailResponse;
import com.clothing.ai.catalog.dto.ProductDtos.ProductSummaryResponse;
import com.clothing.ai.catalog.dto.ProductDtos.VariantResponse;
import com.clothing.ai.catalog.entity.Brand;
import com.clothing.ai.catalog.entity.Category;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.entity.ProductVariant;
import com.clothing.ai.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Maps {@link Product} and its variants/categories/brands into public DTOs.
 *
 * <p>Extracted into its own bean so other services (e.g. RecommendationService)
 * can convert entities without importing ProductService, keeping the dependency
 * graph acyclic.
 */
@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ProductVariantRepository variantRepository;

    // ------------------------------------------------------------------ summary

    /**
     * Load variants from the already-fetched in-memory collection when present
     * (set by a JOIN FETCH query), falling back to a repo hit only when the
     * collection is empty/uninitialized. This avoids an N+1 on product listing pages.
     */
    private List<ProductVariant> variantsFor(Product p) {
        // Hibernate initialises the Set; if it has items we already paid for them
        // via a JOIN FETCH. Only query when there are none loaded.
        try {
            var loaded = p.getVariants();
            if (loaded != null && !loaded.isEmpty()) return loaded.stream().toList();
        } catch (Exception ignored) {
            // proxy not yet initialised — fall through to repo
        }
        return variantRepository.findByProductId(p.getId());
    }

    public ProductSummaryResponse toSummary(Product p) {
        BigDecimal minPrice = variantsFor(p).stream()
                .map(ProductVariant::getEffectivePrice)
                .min(Comparator.naturalOrder())
                .orElse(p.getPrice());
        return new ProductSummaryResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getSku(),
                p.getShortDescription(),
                p.getMainImageUrl(),
                p.getPrice(),
                p.getCompareAtPrice(),
                p.getAverageRating(),
                p.getReviewCount(),
                p.isFeatured(),
                p.isNewArrival(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getBrand() != null ? p.getBrand().getId() : null,
                p.getBrand() != null ? p.getBrand().getName() : null,
                minPrice
        );
    }

    // ------------------------------------------------------------------ detail

    public ProductDetailResponse toDetail(Product p) {
        List<ProductVariant> variants = variantsFor(p);
        List<String> sizes = variants.stream()
                .map(ProductVariant::getSize).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> colors = variants.stream()
                .map(ProductVariant::getColor).filter(Objects::nonNull).distinct().sorted().toList();
        return new ProductDetailResponse(
                toSummary(p),
                p.getDescription(),
                p.getAiGeneratedDescription(),
                p.getImageUrls(),
                p.getTags(),
                variants.stream().map(this::toVariantResponse).toList(),
                sizes,
                colors,
                p.getViewCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    // ------------------------------------------------------------------ variant

    public VariantResponse toVariantResponse(ProductVariant v) {
        return new VariantResponse(
                v.getId(),
                v.getProduct().getId(),
                v.getSku(),
                v.getSize(),
                v.getColor(),
                v.getColorHex(),
                v.getMaterial(),
                v.getPrice(),
                v.getSalePrice(),
                v.getEffectivePrice(),
                v.getStockQuantity(),
                v.getImageUrl(),
                v.getBarcode(),
                v.isActive()
        );
    }

    // ------------------------------------------------------------------ category / brand

    public CategoryResponse toCategoryResponse(Category c) {
        return new CategoryResponse(
                c.getId(),
                c.getName(),
                c.getSlug(),
                c.getDescription(),
                c.getImageUrl(),
                c.getParent() != null ? c.getParent().getId() : null,
                c.getDisplayOrder(),
                c.isActive()
        );
    }

    public BrandResponse toBrandResponse(Brand b) {
        return new BrandResponse(
                b.getId(),
                b.getName(),
                b.getSlug(),
                b.getDescription(),
                b.getLogoUrl(),
                b.isActive()
        );
    }
}
