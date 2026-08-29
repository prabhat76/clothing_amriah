package com.clothing.ai.payment.entity;

import com.clothing.ai.common.base.BaseEntity;
import com.clothing.ai.order.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "transaction_id", unique = true, length = 120)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    public enum PaymentMethod { STRIPE, PAYPAL, COD, APPLE_PAY, GOOGLE_PAY }
    public enum PaymentStatus { PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED, PARTIALLY_REFUNDED }
}
