package com.clothing.ai.admin.controller;

import com.clothing.ai.admin.service.AdminDashboardService;
import com.clothing.ai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin dashboard KPI and summary endpoints.
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin - Dashboard",
        description = "KPI summary cards and metrics for the admin dashboard")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminDashboardController {

    private final AdminDashboardService service;

    @Operation(
            summary = "Dashboard summary KPIs [ADMIN/STAFF]",
            description = """
                    Returns a snapshot of key performance indicators including:

                    | Key | Description |
                    |-----|-------------|
                    | `totalRevenue` | All-time revenue (completed orders) |
                    | `revenueToday` | Revenue generated today |
                    | `ordersTotal` | Total order count |
                    | `ordersPending` | Orders awaiting processing |
                    | `ordersShipped` | Orders currently in transit |
                    | `totalCustomers` | Registered customer count |
                    | `newCustomersToday` | Registrations since midnight |
                    | `totalProducts` | Active product count |
                    | `lowStockProducts` | Products with stock below threshold |
                    | `averageOrderValue` | AOV for the last 30 days |

                    **Cached** for 5 minutes — use `?nocache=true` to force refresh.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "KPI map returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Insufficient role")
    })
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(service.summary());
    }

    @Operation(
            summary = "Revenue chart data [ADMIN/STAFF]",
            description = "Returns daily revenue totals for the last N days (default 30).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Revenue series returned")
    })
    @GetMapping("/revenue-chart")
    public ApiResponse<Map<String, Object>> revenueChart(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(service.revenueChart(days));
    }

    @Operation(
            summary = "Top-selling products [ADMIN/STAFF]",
            description = "Returns the top N products by revenue or units sold for the given period.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Top products list returned")
    })
    @GetMapping("/top-products")
    public ApiResponse<Map<String, Object>> topProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(service.topProducts(limit, days));
    }
}
