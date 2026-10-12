package com.clothing.ai.catalog.repository;

import com.clothing.ai.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductId(UUID productId);
    Optional<ProductVariant> findByProductIdAndSku(UUID productId, String sku);
    Optional<ProductVariant> findBySku(String sku);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(DISTINCT v.product.id) FROM ProductVariant v WHERE v.active = true AND v.stockQuantity <= v.lowStockThreshold")
    long countLowStockProducts();
}
