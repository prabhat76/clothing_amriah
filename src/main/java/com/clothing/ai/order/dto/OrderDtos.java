package com.clothing.ai.order.dto;

import com.clothing.ai.order.entity.Order.OrderStatus;
import com.clothing.ai.order.entity.Order.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order-module data-transfer objects.
 */
public class OrderDtos {

    // ------------------------------------------------------------------ checkout

    @Schema(description = "Checkout request body")
    public record CheckoutRequest(
            @Schema(description = "UUID of the shipping address from the user's address book", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID shippingAddressId,

            @Schema(description = "UUID of the billing address; defaults to shippingAddressId if omitted")
            UUID billingAddressId,

            @Schema(description = "Optional coupon / promo code", example = "SUMMER20")
            String couponCode,

            @Schema(description = "Optional order notes visible to the fulfilment team", maxLength = 1000)
            @Size(max = 1000) String notes,

            @Schema(description = "Payment method: STRIPE | COD | PAYPAL", allowableValues = {"STRIPE", "COD", "PAYPAL"}, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String paymentMethod
    ) {}

    // ------------------------------------------------------------------ responses

    @Schema(description = "A single line item within an order")
    public record OrderItemResponse(
            UUID id,
            UUID variantId,
            UUID productId,
            String productName,
            String size,
            String color,
            String sku,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {}

    @Schema(description = "Snapshot of an address at the time the order was placed")
    public record AddressSnapshot(
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String stateProvince,
            String postalCode,
            String country) {}

    @Schema(description = "Full order detail")
    public record OrderResponse(
            UUID id,
            String orderNumber,
            String status,
            String paymentStatus,
            List<OrderItemResponse> items,
            AddressSnapshot shippingAddress,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shippingCost,
            BigDecimal tax,
            BigDecimal total,
            String currency,
            String trackingNumber,
            String shippingCarrier,
            Instant createdAt,
            Instant shippedAt,
            Instant deliveredAt,
            Instant cancelledAt) {}

    @Schema(description = "Lightweight order tracking response")
    public record OrderTrackingResponse(
            String orderNumber,
            String status,
            String trackingNumber,
            String shippingCarrier,
            Instant shippedAt,
            Instant deliveredAt,
            String estimatedDelivery) {}

    // ------------------------------------------------------------------ admin

    @Schema(description = "Admin request to update order status")
    public record UpdateStatusRequest(
            @Schema(description = "New status", allowableValues = {"CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "REFUNDED"})
            @NotBlank String status,

            @Schema(description = "Tracking number from the shipping carrier")
            String trackingNumber,

            @Schema(description = "Shipping carrier name, e.g. FedEx, DHL")
            String shippingCarrier) {}
}
