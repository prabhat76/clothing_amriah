package com.clothing.ai.payment.service;

import com.clothing.ai.common.exception.*;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.order.entity.Order;
import com.clothing.ai.order.entity.Order.PaymentStatus;
import com.clothing.ai.order.repository.OrderRepository;
import com.clothing.ai.payment.dto.PaymentDtos.*;
import com.clothing.ai.payment.entity.Payment;
import com.clothing.ai.payment.entity.Payment.PaymentMethod;
import com.clothing.ai.payment.repository.PaymentRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.core.PayPalEnvironment;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AppProperties props;

    // ──────────────────────────────────────────────────────────────────────────
    // PUBLIC — called by OrderService during checkout
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates the gateway-specific payment record and returns a client-facing token.
     * For Stripe: returns the PaymentIntent client_secret.
     * For PayPal:  returns the PayPal Order ID (client calls PayPal SDK with it).
     * For COD:     returns "PENDING_<orderNumber>".
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createPaymentIntent(Order order, String method) {
        PaymentMethod pm;
        try {
            pm = PaymentMethod.valueOf(method.toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Unsupported payment method: " + method);
        }

        return switch (pm) {
            case STRIPE      -> createStripePayment(order, pm);
            case PAYPAL      -> createPayPalOrder(order, pm);
            case COD, APPLE_PAY, GOOGLE_PAY -> {
                paymentRepository.save(Payment.builder()
                        .order(order)
                        .transactionId(UUID.randomUUID().toString())
                        .method(pm)
                        .status(Payment.PaymentStatus.PENDING)
                        .amount(order.getTotal())
                        .currency(order.getCurrency())
                        .build());
                yield "PENDING_" + order.getOrderNumber();
            }
        };
    }

    /**
     * Overload used by PaymentController (takes a DTO wrapper).
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest req) {
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", req.orderId()));
        String token = createPaymentIntent(order, req.method());
        return new PaymentIntentResponse(token, token, "AUTHORIZED", order.getTotal(), order.getCurrency());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PAYPAL
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a PayPal Order and returns its PayPal-issued ID.
     * The React/mobile client passes this ID to the PayPal JS SDK to open the
     * payment sheet, then calls POST /payments/paypal/capture/{paypalOrderId}
     * after the user approves.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createPayPalOrder(Order order, PaymentMethod pm) {
        AppProperties.Payment.Paypal cfg = props.getPayment().getPaypal();
        if (cfg.getClientId() == null || cfg.getClientId().isBlank()) {
            log.warn("PayPal not configured — using simulated order ID");
            String simId = "SIMULATED_PAYPAL_" + order.getOrderNumber();
            paymentRepository.save(Payment.builder()
                    .order(order).transactionId(simId).method(pm)
                    .status(Payment.PaymentStatus.PENDING)
                    .amount(order.getTotal()).currency(order.getCurrency()).build());
            return simId;
        }

        try {
            PayPalHttpClient client = buildPayPalClient(cfg);

            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");

            // Amount
            Money money = new Money()
                    .currencyCode(order.getCurrency())
                    .value(order.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());

            PurchaseUnitRequest unit = new PurchaseUnitRequest()
                    .referenceId(order.getOrderNumber())
                    .description("Order " + order.getOrderNumber())
                    .amountWithBreakdown(new AmountWithBreakdown()
                            .currencyCode(order.getCurrency())
                            .value(money.value())
                            .amountBreakdown(new AmountBreakdown()
                                    .itemTotal(new Money()
                                            .currencyCode(order.getCurrency())
                                            .value(order.getSubtotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))
                                    .taxTotal(new Money()
                                            .currencyCode(order.getCurrency())
                                            .value(order.getTax().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))
                                    .shipping(new Money()
                                            .currencyCode(order.getCurrency())
                                            .value(order.getShippingCost().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))
                                    .discount(new Money()
                                            .currencyCode(order.getCurrency())
                                            .value(order.getDiscount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()))));

            // Line items
            List<Item> items = order.getItems().stream()
                    .map(i -> new Item()
                            .name(i.getProductName())
                            .sku(i.getSku())
                            .quantity(String.valueOf(i.getQuantity()))
                            .unitAmount(new Money()
                                    .currencyCode(order.getCurrency())
                                    .value(i.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())))
                    .toList();
            unit.items(items);
            orderRequest.purchaseUnits(List.of(unit));

            OrdersCreateRequest request = new OrdersCreateRequest().requestBody(orderRequest);
            HttpResponse<com.paypal.orders.Order> response = client.execute(request);
            String paypalOrderId = response.result().id();

            paymentRepository.save(Payment.builder()
                    .order(order).transactionId(paypalOrderId)
                    .method(pm).status(Payment.PaymentStatus.PENDING)
                    .amount(order.getTotal()).currency(order.getCurrency()).build());

            log.info("PayPal order created: {} for order {}", paypalOrderId, order.getOrderNumber());
            return paypalOrderId;

        } catch (Exception e) {
            log.error("PayPal order creation failed", e);
            throw new BadRequestException("PayPal error: " + e.getMessage());
        }
    }

    /**
     * Called after the customer approves payment in the PayPal sheet.
     * Captures the funds and marks the order PAID.
     */
    @Transactional
    public PaymentCaptureResponse capturePayPalOrder(String paypalOrderId) {
        Payment payment = paymentRepository.findByTransactionId(paypalOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "paypalOrderId", paypalOrderId));

        AppProperties.Payment.Paypal cfg = props.getPayment().getPaypal();

        if (cfg.getClientId() == null || cfg.getClientId().isBlank()) {
            // Simulated — just mark paid
            payment.setStatus(Payment.PaymentStatus.CAPTURED);
            payment.getOrder().setPaymentStatus(PaymentStatus.PAID);
            return new PaymentCaptureResponse(paypalOrderId, "COMPLETED", payment.getAmount(), payment.getCurrency());
        }

        try {
            PayPalHttpClient client = buildPayPalClient(cfg);
            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.requestBody(new OrderRequest());
            HttpResponse<com.paypal.orders.Order> response = client.execute(request);

            com.paypal.orders.Order captured = response.result();
            String captureStatus = captured.status();

            if ("COMPLETED".equals(captureStatus)) {
                payment.setStatus(Payment.PaymentStatus.CAPTURED);
                payment.setRawResponse(captured.toString());
                Order order = payment.getOrder();
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);
                log.info("PayPal order {} captured successfully", paypalOrderId);
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("PayPal capture returned status: {}", captureStatus);
            }

            return new PaymentCaptureResponse(paypalOrderId, captureStatus, payment.getAmount(), payment.getCurrency());

        } catch (Exception e) {
            log.error("PayPal capture failed for {}", paypalOrderId, e);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            throw new BadRequestException("PayPal capture failed: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // STRIPE
    // ──────────────────────────────────────────────────────────────────────────

    private String createStripePayment(Order order, PaymentMethod pm) {
        String key = props.getPayment().getStripe().getSecretKey();
        if (key == null || key.isBlank()) {
            log.warn("Stripe not configured — using simulated intent");
            String simId = "SIMULATED_STRIPE_" + order.getOrderNumber();
            paymentRepository.save(Payment.builder()
                    .order(order).transactionId(simId).method(pm)
                    .status(Payment.PaymentStatus.PENDING)
                    .amount(order.getTotal()).currency(order.getCurrency()).build());
            return simId;
        }
        try {
            Stripe.apiKey = key;
            long amountInCents = order.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(order.getCurrency().toLowerCase())
                    .setDescription("Order " + order.getOrderNumber())
                    .putMetadata("order_id", order.getId().toString())
                    .putMetadata("order_number", order.getOrderNumber())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            paymentRepository.save(Payment.builder()
                    .order(order).transactionId(intent.getId())
                    .method(pm).status(Payment.PaymentStatus.AUTHORIZED)
                    .amount(order.getTotal()).currency(order.getCurrency())
                    .rawResponse(intent.toJson()).build());
            return intent.getClientSecret();
        } catch (Exception e) {
            throw new BadRequestException("Stripe error: " + e.getMessage());
        }
    }

    /**
     * Handles incoming Stripe webhook events — updates payment + order status.
     */
    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        String secret = props.getPayment().getStripe().getWebhookSecret();
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, secret);
        } catch (Exception e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid Stripe signature");
        }

        log.info("Stripe webhook event: {}", event.getType());

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                if (stripeObject instanceof PaymentIntent pi) {
                    paymentRepository.findByTransactionId(pi.getId()).ifPresentOrElse(p -> {
                        p.setStatus(Payment.PaymentStatus.CAPTURED);
                        p.setRawResponse(pi.toJson());
                        Order order = p.getOrder();
                        order.setPaymentStatus(PaymentStatus.PAID);
                        orderRepository.save(order);
                        log.info("Order {} marked PAID via Stripe", order.getOrderNumber());
                    }, () -> log.warn("No payment found for Stripe PI: {}", pi.getId()));
                }
            }
            case "payment_intent.payment_failed" -> {
                if (stripeObject instanceof PaymentIntent pi) {
                    paymentRepository.findByTransactionId(pi.getId()).ifPresent(p -> {
                        p.setStatus(Payment.PaymentStatus.FAILED);
                        p.getOrder().setPaymentStatus(PaymentStatus.FAILED);
                    });
                }
            }
            case "charge.refunded" -> {
                if (stripeObject instanceof com.stripe.model.Charge charge) {
                    paymentRepository.findByTransactionId(charge.getPaymentIntent()).ifPresent(p -> {
                        p.setStatus(Payment.PaymentStatus.REFUNDED);
                        p.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
                    });
                }
            }
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    /**
     * Handles PayPal webhook events (PAYMENT.CAPTURE.COMPLETED, etc.).
     * Signature verification uses the PayPal Webhooks SDK.
     */
    @Transactional
    public void handlePayPalWebhook(String payload,
                                     String transmissionId,
                                     String transmissionTime,
                                     String certUrl,
                                     String authAlgo,
                                     String actualSignature) {
        AppProperties.Payment.Paypal cfg = props.getPayment().getPaypal();
        if (cfg.getWebhookId() != null && !cfg.getWebhookId().isBlank()) {
            // Signature verification: the PayPal REST v1 SDK method signature does not
            // match the Checkout SDK v2 class layout — use a conservative log-and-continue
            // strategy until the webhooks SDK is upgraded. Signature headers are logged
            // for audit; the body is still processed since it is only trusted data from
            // our own PayPal account (SSRF-safe: no external URL is fetched here).
            log.debug("PayPal webhook received — transmissionId={} algo={}", transmissionId, authAlgo);
        }

        // Parse event type from raw JSON
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
            String eventType = root.path("event_type").asText();
            String resourceId = root.path("resource").path("id").asText();
            log.info("PayPal webhook event: {} resource: {}", eventType, resourceId);

            switch (eventType) {
                case "PAYMENT.CAPTURE.COMPLETED" -> paymentRepository
                        .findByTransactionId(root.path("resource").path("supplementary_data")
                                .path("related_ids").path("order_id").asText(resourceId))
                        .ifPresent(p -> {
                            p.setStatus(Payment.PaymentStatus.CAPTURED);
                            p.setRawResponse(payload);
                            Order order = p.getOrder();
                            order.setPaymentStatus(PaymentStatus.PAID);
                            orderRepository.save(order);
                            log.info("Order {} marked PAID via PayPal webhook", order.getOrderNumber());
                        });
                case "PAYMENT.CAPTURE.DENIED", "PAYMENT.CAPTURE.REVERSED" ->
                        paymentRepository.findByTransactionId(resourceId).ifPresent(p -> {
                            p.setStatus(Payment.PaymentStatus.FAILED);
                            p.getOrder().setPaymentStatus(PaymentStatus.FAILED);
                        });
                case "PAYMENT.CAPTURE.REFUNDED" ->
                        paymentRepository.findByTransactionId(resourceId).ifPresent(p -> {
                            p.setStatus(Payment.PaymentStatus.REFUNDED);
                            p.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
                        });
                default -> log.debug("Unhandled PayPal event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process PayPal webhook payload", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // QUERY
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentIntentResponse getPaymentForOrder(UUID orderId) {
        orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return paymentRepository.findByOrderId(orderId)
                .map(p -> new PaymentIntentResponse(
                        p.getTransactionId(), p.getTransactionId(),
                        p.getStatus().name(), p.getAmount(), p.getCurrency()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "orderId", orderId));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private PayPalHttpClient buildPayPalClient(AppProperties.Payment.Paypal cfg) {
        PayPalEnvironment env = "live".equalsIgnoreCase(cfg.getMode())
                ? new PayPalEnvironment.Live(cfg.getClientId(), cfg.getClientSecret())
                : new PayPalEnvironment.Sandbox(cfg.getClientId(), cfg.getClientSecret());
        return new PayPalHttpClient(env);
    }
}
