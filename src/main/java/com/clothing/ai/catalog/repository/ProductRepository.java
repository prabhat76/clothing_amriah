package com.clothing.ai.catalog.repository;

import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.entity.Product.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByActiveTrueAndStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByActiveTrueAndBrandId(UUID brandId, Pageable pageable);

    Page<Product> findByActiveTrueAndFeaturedTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndNewArrivalTrue(Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
          AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:tag IS NULL OR :tag MEMBER OF p.tags)
        """)
    Page<Product> search(@Param("q") String q,
                         @Param("categoryId") UUID categoryId,
                         @Param("brandId") UUID brandId,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("tag") String tag,
                         Pageable pageable);

    List<Product> findTop12ByActiveTrueOrderBySalesCountDesc();

    List<Product> findTop12ByActiveTrueOrderByViewCountDesc();

    List<Product> findTop20ByActiveTrueAndNewArrivalTrueOrderByCreatedAtDesc();

    long countByCategoryIdAndActiveTrue(UUID categoryId);

    long countByBrandIdAndActiveTrue(UUID brandId);
}
