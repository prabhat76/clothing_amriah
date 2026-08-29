package com.clothing.ai.notification.service;

import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.notification.dto.NotificationDtos.*;
import com.clothing.ai.notification.entity.Notification;
import com.clothing.ai.notification.entity.Notification.NotificationType;
import com.clothing.ai.notification.repository.NotificationRepository;
import com.clothing.ai.order.entity.Order;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    @Nullable
    private JavaMailSender mailSender;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               AppProperties props) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.props = props;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async("taskExecutor")
    public void sendOrderConfirmation(Order order) {
        create(order.getUser(), NotificationType.ORDER,
                "Order Confirmed",
                "Your order " + order.getOrderNumber() + " has been placed successfully.",
                order.getOrderNumber(), true);
    }

    @Async("taskExecutor")
    public void sendOrderStatusUpdate(Order order) {
        create(order.getUser(), NotificationType.ORDER,
                "Order " + order.getStatus().name(),
                "Order " + order.getOrderNumber() + " is now " + order.getStatus().name()
                + (order.getTrackingNumber() != null ? ". Tracking: " + order.getTrackingNumber() : ""),
                order.getOrderNumber(), true);
    }

    @Async("taskExecutor")
    public void sendBackInStock(UUID userId, String productName) {
        userRepository.findById(userId).ifPresent(u ->
                create(u, NotificationType.BACK_IN_STOCK, "Back in Stock", productName + " is back in stock.", productName, true));
    }

    @Async("taskExecutor")
    public void sendPriceDrop(UUID userId, String productName, String price) {
        userRepository.findById(userId).ifPresent(u ->
                create(u, NotificationType.PRICE_DROP, "Price Drop", productName + " is now " + price, productName, true));
    }

    @Transactional
    public void create(User user, NotificationType type, String title, String body, String payload, boolean sendEmail) {
        Notification n = Notification.builder()
                .user(user).type(type).title(title).body(body).payload(payload).build();
        notificationRepository.save(n);
        if (sendEmail && user.getEmail() != null) sendEmail(user.getEmail(), title, body);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> my(UUID userId, int page, int size) {
        Page<Notification> p = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PageResponse.from(p, this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID id, UUID userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                n.setReadAt(Instant.now());
            }
        });
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500))
                .forEach(n -> { n.setRead(true); n.setReadAt(Instant.now()); });
    }

    @Transactional
    public void delete(UUID id, UUID userId) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (!n.getUser().getId().equals(userId)) {
                throw new com.clothing.ai.common.exception.ForbiddenException("Not your notification");
            }
            notificationRepository.delete(n);
        });
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.debug("JavaMailSender not configured — skipping email to {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(props.getNotifications().getEmail().getFrom());
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Email send failed to {}: {}", to, e.getMessage());
        }
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(), n.getBody(), n.getPayload(),
                n.isRead(), n.getReadAt(), n.getChannel(), n.getCreatedAt());
    }
}
