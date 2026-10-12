package com.clothing.ai.admin.service;

import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.catalog.repository.ProductVariantRepository;
import com.clothing.ai.order.entity.Order.OrderStatus;
import com.clothing.ai.order.repository.OrderRepository;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant last30d = Instant.now().minus(30, ChronoUnit.DAYS);

        BigDecimal totalRevenue = orderRepository.totalRevenue();
        BigDecimal revenueToday = orderRepository.revenueFrom(startOfToday);
        BigDecimal aov30d = orderRepository.avgOrderValueFrom(last30d);

        Map<String, Object> map = new HashMap<>();
        map.put("totalRevenue", totalRevenue != null ? totalRevenue.doubleValue() : 0.0);
        map.put("revenueToday", revenueToday != null ? revenueToday.doubleValue() : 0.0);
        map.put("ordersTotal", orderRepository.count());
        map.put("ordersPending", orderRepository.countByStatus(OrderStatus.PENDING));
        map.put("ordersShipped", orderRepository.countByStatus(OrderStatus.SHIPPED));
        map.put("totalCustomers", userRepository.count());
        map.put("newCustomersToday", userRepository.countUsersFrom(startOfToday));
        map.put("totalProducts", productRepository.countByActiveTrue());
        map.put("lowStockProducts", variantRepository.countLowStockProducts());
        map.put("averageOrderValue", aov30d != null ? aov30d.doubleValue() : 0.0);
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> revenueChart(int days) {
        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("totalRevenue", orderRepository.revenueFrom(from));
        result.put("totalOrders", orderRepository.countOrdersFrom(from));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> topProducts(int limit, int days) {
        Map<String, Object> result = new HashMap<>();
        var products = productRepository.findTop12ByActiveTrueOrderBySalesCountDesc().stream()
                .limit(limit)
                .map(p -> Map.of(
                        "id", p.getId(), "name", p.getName(),
                        "salesCount", p.getSalesCount(), "price", p.getPrice()))
                .toList();
        result.put("products", products);
        result.put("days", days);
        return result;
    }
}
