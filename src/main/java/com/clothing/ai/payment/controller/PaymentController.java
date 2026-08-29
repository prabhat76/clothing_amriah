package com.clothing.ai.payment.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.payment.dto.PaymentDtos.*;
import com.clothing.ai.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment endpoints — Stripe PaymentIntents and webhook ingestion.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe payment intents, webhook handling, COD / PayPal support")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Create a Stripe PaymentIntent",
            description = """
                    Creates (or retrieves an idempotent) Stripe PaymentIntent for the specified order.
                    The `clientSecret` in the response is passed to the Stripe.js SDK on the client.

                    **Only call this when `paymentMethod = STRIPE` during checkout.**
                    For COD / PayPal the order is finalised by the checkout endpoint itself.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "PaymentIntent created",
                    content = @Content(schema = @Schema(implementation = PaymentIntentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid order ID or payment already completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "Stripe service unavailable")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/create-intent")
    public ApiResponse<PaymentIntentResponse> createIntent(@Valid @RequestBody CreatePaymentIntentRequest req) {
        return ApiResponse.success(paymentService.createPaymentIntent(req));
    }

    @Operation(
            summary = "Stripe webhook receiver",
            description = """
                    Receives `POST` events from Stripe's event bus.

                    **Do NOT call this endpoint manually** — it is for Stripe's servers only.
                    Stripe-Signature header validation is enforced using the configured webhook secret.

                    Handled events:
                    - `payment_intent.succeeded` → marks order as PAID
                    - `payment_intent.payment_failed` → marks order as PAYMENT_FAILED
                    - `charge.refunded` → marks order as REFUNDED
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Event acknowledged"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid or missing Stripe-Signature"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422", description = "Event type not handled (silently acknowledged)")
    })
    @SecurityRequirements  // public — verified via Stripe-Signature header
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @Parameter(description = "Stripe webhook signature header", required = true)
            @RequestHeader(value = "Stripe-Signature", required = false) String sig) {
        paymentService.handleWebhook(payload, sig);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get payment status for an order",
            description = "Returns the current payment status and transaction ID for an order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Payment status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentIntentResponse> getForOrder(
            @Parameter(description = "Order UUID") @PathVariable java.util.UUID orderId) {
        return ApiResponse.success(paymentService.getPaymentForOrder(orderId));
    }
}
