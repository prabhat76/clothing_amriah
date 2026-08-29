package com.clothing.ai.review.repository;

import com.clothing.ai.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Page<Review> findByProductIdAndApprovedTrue(UUID productId, Pageable pageable);
    Page<Review> findByProductId(UUID productId, Pageable pageable);
    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);
    long countByProductId(UUID productId);
}
