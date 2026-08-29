package com.clothing.ai.order.service;

import com.clothing.ai.cart.dto.CartDtos.CartResponse;
import com.clothing.ai.cart.service.CartService;
import com.clothing.ai.catalog.entity.ProductVariant;
import com.clothing.ai.catalog.repository.ProductVariantRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.notification.service.NotificationService;
import com.clothing.ai.order.dto.OrderDtos.*;
import com.clothing.ai.order.entity.Order;
import com.clothing.ai.order.entity.Order.OrderStatus;
import com.clothing.ai.order.entity.Order.PaymentStatus;
import com.clothing.ai.order.entity.OrderItem;
import com.clothing.ai.order.repository.OrderRepository;
import com.clothing.ai.payment.service.PaymentService;
import com.clothing.ai.user.entity.Address;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.AddressRepository;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse checkout(UUID userId, CheckoutRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","id",userId));
        Address shipping = addressRepository.findById(req.shippingAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address","id",req.shippingAddressId()));
        if (!shipping.getUser().getId().equals(userId)) throw new ForbiddenException("Not your address");
        Address billing = req.billingAddressId() != null ? addressRepository.findById(req.billingAddressId()).orElse(shipping) : shipping;

        CartResponse cart = cartService.getOrCreateCart(userId);
        if (cart.items().isEmpty()) throw new BadRequestException("Cart is empty");

        // Validate and reserve stock
        for (var i : cart.items()) {
            ProductVariant v = variantRepository.findById(i.variantId()).orElseThrow();
            if (v.getStockQuantity() < i.quantity()) throw new BadRequestException("Insufficient stock for SKU " + v.getSku());
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user).shippingAddress(shipping).billingAddress(billing)
                .status(OrderStatus.PENDING).paymentStatus(PaymentStatus.PENDING)
                .subtotal(cart.subtotal()).discount(cart.discount()).shippingCost(cart.shipping())
                .tax(cart.tax()).total(cart.total()).currency("USD")
                .couponCode(req.couponCode()).notes(req.notes())
                .build();

        for (var ci : cart.items()) {
            ProductVariant v = variantRepository.findById(ci.variantId()).orElseThrow();
            OrderItem item = OrderItem.builder()
                    .order(order).variant(v)
                    .productName(ci.productName()).size(ci.size()).color(ci.color()).sku(ci.sku())
                    .imageUrl(ci.imageUrl()).unitPrice(ci.unitPrice())
                    .quantity(ci.quantity()).lineTotal(ci.lineTotal())
                    .build();
            order.getItems().add(item);
            v.setStockQuantity(v.getStockQuantity() - ci.quantity());
        }

        order = orderRepository.save(order);

        // Initiate payment
        try {
            String paymentIntent = paymentService.createPaymentIntent(order, req.paymentMethod());
            order.setPaymentStatus(PaymentStatus.AUTHORIZED);
        } catch (Exception e) {
            log.warn("Payment init failed: {}", e.getMessage());
        }

        cartService.clearCart(userId);
        notificationService.sendOrderConfirmation(order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> myOrders(UUID userId, int page, int size) {
        return PageResponse.from(orderRepository.findByUserId(userId, PageRequest.of(page, size)), this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByNumber(String orderNumber) {
        Order o = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new ResourceNotFoundException("Order","number",orderNumber));
        return toResponse(o);
    }

    @Transactional
    public OrderResponse cancel(UUID userId, String orderNumber) {
        Order o = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new ResourceNotFoundException("Order","number",orderNumber));
        if (!o.getUser().getId().equals(userId)) throw new ForbiddenException("Not your order");
        if (o.getStatus() == OrderStatus.SHIPPED || o.getStatus() == OrderStatus.DELIVERED)
            throw new BadRequestException("Order cannot be cancelled in status " + o.getStatus());
        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelledAt(Instant.now());
        // restore stock
        for (OrderItem item : o.getItems()) {
            ProductVariant v = item.getVariant();
            v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
        }
        return toResponse(o);
    }

    @Transactional
    public OrderResponse updateStatus(String orderNumber, String status, String tracking, String carrier) {
        Order o = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order","number",orderNumber));
        OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
        o.setStatus(newStatus);
        if (newStatus == OrderStatus.SHIPPED) {
            o.setShippedAt(Instant.now());
            if (tracking != null) o.setTrackingNumber(tracking);
            if (carrier != null) o.setShippingCarrier(carrier);
        } else if (newStatus == OrderStatus.DELIVERED) {
            o.setDeliveredAt(Instant.now());
        } else if (newStatus == OrderStatus.CONFIRMED) {
            o.setPaymentStatus(PaymentStatus.PAID);
        } else if (newStatus == OrderStatus.CANCELLED) {
            o.setCancelledAt(Instant.now());
        }
        notificationService.sendOrderStatusUpdate(o);
        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status, PageRequest.of(0, 100)).map(this::toResponse).getContent();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findByStatusPaged(OrderStatus status, int page, int size) {
        return PageResponse.from(
                orderRepository.findByStatus(status, PageRequest.of(page, size)), this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrder(UUID userId, String orderNumber) {
        Order o = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order","number",orderNumber));
        if (!o.getUser().getId().equals(userId)) throw new ForbiddenException("Not your order");
        return new OrderTrackingResponse(
                o.getOrderNumber(), o.getStatus().name(),
                o.getTrackingNumber(), o.getShippingCarrier(),
                o.getShippedAt(), o.getDeliveredAt(), null);
    }

    private String generateOrderNumber() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "CL-" + ts + "-" + (int)(Math.random() * 9000 + 1000);
    }

    public OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream().map(i -> new OrderItemResponse(
                i.getId(), i.getVariant().getId(), i.getVariant().getProduct().getId(),
                i.getProductName(), i.getSize(), i.getColor(), i.getSku(), i.getImageUrl(),
                i.getUnitPrice(), i.getQuantity(), i.getLineTotal()
        )).toList();
        Address a = o.getShippingAddress();
        AddressSnapshot addr = a == null ? null : new AddressSnapshot(a.getFullName(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getStateProvince(), a.getPostalCode(), a.getCountry());
        return new OrderResponse(o.getId(), o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus().name(),
                items, addr, o.getSubtotal(), o.getDiscount(), o.getShippingCost(), o.getTax(),
                o.getTotal(), o.getCurrency(), o.getTrackingNumber(), o.getShippingCarrier(),
                o.getCreatedAt(), o.getShippedAt(), o.getDeliveredAt(), o.getCancelledAt());
    }
}
