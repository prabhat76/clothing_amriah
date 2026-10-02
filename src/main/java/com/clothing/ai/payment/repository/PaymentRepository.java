package com.clothing.ai.payment.repository;

import com.clothing.ai.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Returns the most-recently-created payment for an order.
     * Orders may have multiple payment rows when a first attempt fails and the
     * customer retries — we always want the latest one for status queries.
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<Payment> findByOrderId(@Param("orderId") UUID orderId);

    List<Payment> findAllByOrderId(UUID orderId);
}
