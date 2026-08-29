package com.clothing.ai.payment.service;

import com.clothing.ai.common.exception.*;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.order.entity.Order;
import com.clothing.ai.order.repository.OrderRepository;
import com.clothing.ai.payment.dto.PaymentDtos.*;
import com.clothing.ai.payment.entity.Payment;
import com.clothing.ai.payment.entity.Payment.PaymentMethod;
import com.clothing.ai.payment.entity.Payment.PaymentStatus;
import com.clothing.ai.payment.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AppProperties props;

    @Transactional
    public String createPaymentIntent(Order order, String method) {
        PaymentMethod pm;
        try { pm = PaymentMethod.valueOf(method.toUpperCase()); }
        catch (Exception e) { throw new BadRequestException("Unsupported payment method: " + method); }

        return switch (pm) {
            case STRIPE -> createStripePayment(order, pm);
            case COD, PAYPAL, APPLE_PAY, GOOGLE_PAY -> {
                paymentRepository.save(Payment.builder()
                        .order(order).transactionId(UUID.randomUUID().toString())
                        .method(pm).status(PaymentStatus.PENDING)
                        .amount(order.getTotal()).currency(order.getCurrency()).build());
                yield "PENDING_" + order.getOrderNumber();
            }
        };
    }

    private String createStripePayment(Order order, PaymentMethod pm) {
        String key = props.getPayment().getStripe().getSecretKey();
        if (key == null || key.isBlank()) {
            log.warn("Stripe not configured, using simulated intent");
            return "SIMULATED_" + order.getOrderNumber();
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
                    .method(pm).status(PaymentStatus.AUTHORIZED)
                    .amount(order.getTotal()).currency(order.getCurrency())
                    .rawResponse(intent.toJson()).build());
            return intent.getId();
        } catch (Exception e) {
            throw new BadRequestException("Stripe error: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest req) {
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order","id",req.orderId()));
        String tx = createPaymentIntent(order, req.method());
        return new PaymentIntentResponse(tx, tx, "AUTHORIZED", order.getTotal(), order.getCurrency());
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        String secret = props.getPayment().getStripe().getWebhookSecret();
        try {
            com.stripe.net.Webhook.constructEvent(payload, sigHeader, secret);
            log.info("Stripe webhook received");
        } catch (Exception e) {
            log.error("Webhook signature verification failed", e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse getPaymentForOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order","id",orderId));
        return paymentRepository.findByOrderId(orderId)
                .map(p -> new PaymentIntentResponse(
                        p.getTransactionId(), p.getTransactionId(),
                        p.getStatus().name(), p.getAmount(), p.getCurrency()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment","orderId",orderId));
    }
}
