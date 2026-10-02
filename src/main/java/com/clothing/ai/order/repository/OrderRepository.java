package com.clothing.ai.order.repository;

import com.clothing.ai.order.entity.Order;
import com.clothing.ai.order.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product
        WHERE o.orderNumber = :orderNumber
        """)
    Optional<Order> findByOrderNumber(@Param("orderNumber") String orderNumber);

    // Count query for pagination (no fetch joins needed)
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product
        WHERE o.user.id = :userId
        ORDER BY o.createdAt DESC
        """)
    List<Order> findByUserIdFetched(@Param("userId") UUID userId);

    // Keep original for any callers that use plain paging
    Page<Order> findByUserId(UUID userId, Pageable pageable);

    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.variant v
        LEFT JOIN FETCH v.product
        WHERE o.status = :status
        """)
    List<Order> findByStatusFetched(@Param("status") OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);
}

