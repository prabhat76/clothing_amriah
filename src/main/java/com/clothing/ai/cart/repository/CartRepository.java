package com.clothing.ai.cart.repository;

import com.clothing.ai.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    @Query("""
        SELECT DISTINCT c FROM Cart c
        LEFT JOIN FETCH c.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product
        WHERE c.user.id = :userId
        """)
    Optional<Cart> findByUserId(@Param("userId") UUID userId);
}
