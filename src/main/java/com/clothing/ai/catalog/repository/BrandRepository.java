package com.clothing.ai.catalog.repository;

import com.clothing.ai.catalog.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Optional<Brand> findBySlug(String slug);
}
