package com.clothing.ai.cart.repository;

import com.clothing.ai.cart.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    Optional<Wishlist> findByUserIdAndProductId(UUID userId, UUID productId);

    Page<Wishlist> findByUserId(UUID userId, Pageable pageable);

    /** Ordered list used by WishlistService.list(UUID) */
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserIdAndProductId(UUID userId, UUID productId);
}
