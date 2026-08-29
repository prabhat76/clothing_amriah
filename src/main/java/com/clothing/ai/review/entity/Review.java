package com.clothing.ai.review.entity;

import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.common.base.BaseEntity;
import com.clothing.ai.order.entity.OrderItem;
import com.clothing.ai.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_product", columnList = "product_id"),
    @Index(name = "idx_review_user", columnList = "user_id")
}, uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(nullable = false)
    private int rating;  // 1-5

    @Column(length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "size_fit")
    private Integer sizeFit;  // 1-5

    @Column(name = "quality")
    private Integer quality;

    @Column(name = "verified_purchase", nullable = false)
    private boolean verifiedPurchase = false;

    @Column(nullable = false)
    private boolean approved = true;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;
}
