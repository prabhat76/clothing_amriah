package com.clothing.ai.payment.repository;

import com.clothing.ai.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(String transactionId);
    Optional<Payment> findByOrderId(UUID orderId);
}
