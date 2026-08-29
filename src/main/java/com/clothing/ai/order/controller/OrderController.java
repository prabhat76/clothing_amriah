package com.clothing.ai.order.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.order.dto.OrderDtos.*;
import com.clothing.ai.order.service.OrderService;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Customer order management — checkout, history, tracking, cancellation.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Checkout, order history, order tracking and cancellation")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Checkout — place an order from the cart",
            description = """
                    Converts the authenticated user's cart into an order.

                    **Flow:**
                    1. Cart must not be empty.
                    2. Stock is reserved atomically — if any variant is out-of-stock the entire checkout fails.
                    3. If `paymentMethod = STRIPE` a Stripe PaymentIntent is created; poll `/payments/create-intent`.
                    4. If `paymentMethod = COD` the order goes straight to `CONFIRMED`.

                    **Coupon codes** are validated and the discount is applied before the order total is set.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Order placed successfully",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Cart is empty, invalid address, or coupon invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "One or more variants are out of stock")
    })
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest req) {
        return ApiResponse.success("Order placed successfully",
                orderService.checkout(SecurityUtils.currentUserId(), req));
    }

    @Operation(
            summary = "List my orders",
            description = "Returns the authenticated user's order history, newest first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Paginated order list returned")
    })
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> myOrders(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 50)") @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(orderService.myOrders(SecurityUtils.currentUserId(), page, size));
    }

    @Operation(
            summary = "Get order by order number",
            description = """
                    Returns the full order detail including all line items and status history.

                    **Access:** authenticated users may only view their own orders.
                    Admin users may view any order.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Order detail returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Order belongs to a different user")
    })
    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> getByNumber(
            @Parameter(description = "Order number, e.g. `CLO-2024-000001`") @PathVariable String orderNumber) {
        return ApiResponse.success(orderService.getByNumber(orderNumber));
    }

    @Operation(
            summary = "Cancel an order",
            description = """
                    Cancels an order that is still in a cancellable state
                    (`PENDING` or `CONFIRMED`).

                    **Side effects:**
                    - Stock quantities are restored for all reserved variants.
                    - If the order was paid, a Stripe refund is initiated automatically.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Order cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Order is already shipped/delivered — cannot cancel"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Order belongs to a different user")
    })
    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {
        return ApiResponse.success("Order cancelled",
                orderService.cancel(SecurityUtils.currentUserId(), orderNumber));
    }

    @Operation(
            summary = "Track order status",
            description = "Returns the current status, tracking number, and carrier for an order.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Tracking info returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderNumber}/tracking")
    public ApiResponse<OrderTrackingResponse> track(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {
        return ApiResponse.success(orderService.trackOrder(SecurityUtils.currentUserId(), orderNumber));
    }
}
