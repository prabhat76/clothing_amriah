package com.clothing.ai.catalog.entity;

import com.clothing.ai.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id","sku"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 60)
    private String sku;

    @Column(name = "size", length = 20)
    private String size;

    @Column(name = "color", length = 40)
    private String color;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    @Column(name = "material", length = 80)
    private String material;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold = 5;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "barcode", length = 60)
    private String barcode;

    @Column(nullable = false)
    private boolean active = true;

    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : price;
    }

    public boolean inStock() { return stockQuantity > 0; }
}
