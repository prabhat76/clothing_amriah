package com.clothing.ai.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Payment module DTOs.
 */
public class PaymentDtos {

    @Schema(description = "Create a payment intent / PayPal order for an existing order")
    public record CreatePaymentIntentRequest(
            @Schema(description = "UUID of the order to pay for", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull UUID orderId,

            @Schema(description = "Payment method",
                    allowableValues = {"STRIPE", "COD", "PAYPAL", "APPLE_PAY", "GOOGLE_PAY"},
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String method) {}

    @Schema(description = "Payment intent / PayPal order creation response")
    public record PaymentIntentResponse(
            @Schema(description = "Internal transaction ID, Stripe PaymentIntent ID, or PayPal Order ID")
            String transactionId,

            @Schema(description = "Stripe client_secret — pass to Stripe.js to complete card payment. " +
                                   "For PayPal, pass transactionId to the PayPal JS SDK. Null for COD.")
            String clientSecret,

            @Schema(description = "Payment status", example = "AUTHORIZED")
            String status,

            @Schema(description = "Amount charged", example = "79.99")
            BigDecimal amount,

            @Schema(description = "3-letter ISO currency code", example = "USD")
            String currency) {}

    @Schema(description = "PayPal order capture response")
    public record PaymentCaptureResponse(
            @Schema(description = "PayPal Order ID")
            String paypalOrderId,

            @Schema(description = "PayPal capture status — COMPLETED means funds secured")
            String status,

            @Schema(description = "Amount captured")
            BigDecimal amount,

            @Schema(description = "Currency")
            String currency) {}

    @Schema(description = "Raw Stripe webhook event — Stripe sends this automatically")
    public record StripeWebhookRequest(String id, String type, Map<String, Object> data) {}
}
