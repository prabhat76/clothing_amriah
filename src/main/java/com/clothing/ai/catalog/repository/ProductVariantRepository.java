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
}
