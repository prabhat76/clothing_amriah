package com.clothing.ai.catalog.entity;

import com.clothing.ai.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_slug", columnList = "slug", unique = true),
    @Index(name = "idx_product_active", columnList = "active"),
    @Index(name = "idx_product_brand", columnList = "brand_id"),
    @Index(name = "idx_product_category", columnList = "category_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "main_image_url", length = 500)
    private String mainImageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private Set<String> imageUrls = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag", length = 60)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.DRAFT;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(name = "is_new_arrival", nullable = false)
    private boolean newArrival = false;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "sales_count", nullable = false)
    private int salesCount = 0;

    // AI generated fields
    @Column(name = "ai_embedding_id", length = 60)
    private String aiEmbeddingId;

    @Column(name = "ai_generated_description", columnDefinition = "TEXT")
    private String aiGeneratedDescription;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    public enum ProductStatus { DRAFT, ACTIVE, OUT_OF_STOCK, DISCONTINUED, ARCHIVED }
}
