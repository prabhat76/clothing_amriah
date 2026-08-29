package com.clothing.ai.notification.entity;

import com.clothing.ai.common.base.BaseEntity;
import com.clothing.ai.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private java.time.Instant readAt;

    @Column(name = "channel", length = 20)
    private String channel = "IN_APP";

    public enum NotificationType { ORDER, PROMOTION, BACK_IN_STOCK, PRICE_DROP, REVIEW, SYSTEM, AI_RECOMMENDATION }
}
