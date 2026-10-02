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

import java.util.UUID;

/**
 * Payment endpoints — Stripe PaymentIntents, PayPal Orders, and webhook ingestion.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Stripe payment intents, PayPal orders, webhook handling, COD support")
public class PaymentController {

    private final PaymentService paymentService;

    // ──────────────────────────────────────────────────────────────────────────
    // CREATE INTENT (Stripe client_secret OR PayPal order ID)
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Create a payment intent / PayPal order",
            description = """
                    - **STRIPE**: creates a Stripe PaymentIntent; the `clientSecret` in the response
                      is passed to the `Stripe.js` SDK on the client to open the payment sheet.
                    - **PAYPAL**: creates a PayPal Order; the `transactionId` is the PayPal Order ID —
                      pass it to the PayPal JS SDK (`paypal.Buttons({ createOrder: () => transactionId })`).
                      After the user approves, call `POST /payments/paypal/capture/{transactionId}`.
                    - **COD**: immediately confirmed — no client action needed.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Intent / Order created",
                    content = @Content(schema = @Schema(implementation = PaymentIntentResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid order ID or unsupported method"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/create-intent")
    public ApiResponse<PaymentIntentResponse> createIntent(
            @Valid @RequestBody CreatePaymentIntentRequest req) {
        return ApiResponse.success(paymentService.createPaymentIntent(req));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PAYPAL — capture after buyer approval
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Capture an approved PayPal order",
            description = """
                    Call this endpoint AFTER the PayPal JS SDK returns a confirmed `orderID` from
                    `onApprove`. This triggers the actual fund capture on PayPal and marks the
                    order as PAID in the database.

                    Frontend flow:
                    ```js
                    paypal.Buttons({
                      createOrder: () => fetch('/api/payments/create-intent', { method: 'POST', body: ... })
                                         .then(r => r.json()).then(d => d.data.transactionId),
                      onApprove: (data) => fetch('/api/payments/paypal/capture/' + data.orderID, { method: 'POST' })
                    }).render('#paypal-button');
                    ```
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Capture result",
                    content = @Content(schema = @Schema(implementation = PaymentCaptureResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "PayPal order not found in database"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Capture failed or order already captured")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/paypal/capture/{paypalOrderId}")
    public ApiResponse<PaymentCaptureResponse> capturePayPal(
            @Parameter(description = "PayPal Order ID returned from create-intent")
            @PathVariable String paypalOrderId) {
        return ApiResponse.success(paymentService.capturePayPalOrder(paypalOrderId));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PAYPAL WEBHOOK
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "PayPal webhook receiver",
            description = """
                    Receives asynchronous events from PayPal (PAYMENT.CAPTURE.COMPLETED, etc.).
                    **Do NOT call this manually.** Configure this URL in your PayPal Developer
                    Dashboard → Apps & Credentials → Webhooks.

                    Handled events:
                    - `PAYMENT.CAPTURE.COMPLETED` → marks order PAID
                    - `PAYMENT.CAPTURE.DENIED` / `REVERSED` → marks order FAILED
                    - `PAYMENT.CAPTURE.REFUNDED` → marks order REFUNDED
                    """)
    @SecurityRequirements  // public — verified via PayPal signature headers
    @PostMapping("/paypal/webhook")
    public ResponseEntity<Void> paypalWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-ID",   required = false) String transmissionId,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-TIME", required = false) String transmissionTime,
            @RequestHeader(value = "PAYPAL-CERT-URL",          required = false) String certUrl,
            @RequestHeader(value = "PAYPAL-AUTH-ALGO",         required = false) String authAlgo,
            @RequestHeader(value = "PAYPAL-TRANSMISSION-SIG",  required = false) String signature) {
        paymentService.handlePayPalWebhook(payload, transmissionId, transmissionTime, certUrl, authAlgo, signature);
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // STRIPE WEBHOOK
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(
            summary = "Stripe webhook receiver",
            description = """
                    Receives POST events from Stripe's event bus. **Do NOT call manually.**
                    Stripe-Signature header validation enforced via configured webhook secret.

                    Handled events:
                    - `payment_intent.succeeded` → marks order PAID
                    - `payment_intent.payment_failed` → marks order FAILED
                    - `charge.refunded` → marks order REFUNDED
                    """)
    @SecurityRequirements  // public — verified via Stripe-Signature header
    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @Parameter(description = "Stripe webhook signature header")
            @RequestHeader(value = "Stripe-Signature", required = false) String sig) {
        paymentService.handleStripeWebhook(payload, sig);
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // QUERY
    // ──────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Get payment status for an order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Payment status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order or payment not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/order/{orderId}")
    public ApiResponse<PaymentIntentResponse> getForOrder(
            @Parameter(description = "Order UUID") @PathVariable UUID orderId) {
        return ApiResponse.success(paymentService.getPaymentForOrder(orderId));
    }
}
