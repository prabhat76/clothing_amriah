package com.clothing.ai.order.controller;

import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.order.dto.OrderDtos.*;
import com.clothing.ai.order.entity.Order.OrderStatus;
import com.clothing.ai.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only order management endpoints.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin - Orders", description = "Admin order management and status transitions")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(
            summary = "List orders by status [ADMIN/STAFF]",
            description = """
                    Returns all orders with the given status, newest first.

                    | Status | Description |
                    |--------|-------------|
                    | PENDING | Placed, awaiting payment confirmation |
                    | CONFIRMED | Payment confirmed, awaiting processing |
                    | PROCESSING | Being picked/packed |
                    | SHIPPED | Dispatched to carrier |
                    | DELIVERED | Received by customer |
                    | CANCELLED | Cancelled by customer or admin |
                    | REFUNDED | Refund issued |
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Orders returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid status value")
    })
    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> byStatus(
            @Parameter(description = "Order status filter") @RequestParam OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.success(orderService.findByStatusPaged(status, page, size));
    }

    @Operation(
            summary = "Update order status [ADMIN/STAFF]",
            description = """
                    Transitions an order to a new status and optionally sets tracking info.

                    **Allowed transitions:**
                    - CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                    - Any non-terminal → CANCELLED (admin only)

                    Setting status to `SHIPPED` requires `trackingNumber`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid status transition"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found")
    })
    @PatchMapping("/{orderNumber}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @Parameter(description = "Order number") @PathVariable String orderNumber,
            @Valid @RequestBody UpdateStatusRequest req) {
        return ApiResponse.success("Status updated",
                orderService.updateStatus(orderNumber, req.status(), req.trackingNumber(), req.shippingCarrier()));
    }

    @Operation(
            summary = "Get order detail [ADMIN/STAFF]",
            description = "Returns full detail of any order by order number, regardless of customer ownership.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Order returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> get(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {
        return ApiResponse.success(orderService.getByNumber(orderNumber));
    }
}
