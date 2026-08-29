package com.clothing.ai.admin.service;

import com.clothing.ai.catalog.repository.ProductRepository;
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
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Instant last7d = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant last30d = Instant.now().minus(30, ChronoUnit.DAYS);

        Map<String, Object> map = new HashMap<>();
        map.put("totalOrders", orderRepository.count());
        map.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        map.put("processingOrders", orderRepository.countByStatus(OrderStatus.PROCESSING));
        map.put("shippedOrders", orderRepository.countByStatus(OrderStatus.SHIPPED));
        map.put("deliveredOrders", orderRepository.countByStatus(OrderStatus.DELIVERED));
        map.put("cancelledOrders", orderRepository.countByStatus(OrderStatus.CANCELLED));
        map.put("totalProducts", productRepository.count());
        map.put("activeProducts", productRepository.findAll().stream().filter(p -> p.isActive()).count());
        map.put("totalUsers", userRepository.count());
        map.put("generatedAt", Instant.now());
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> revenueChart(int days) {
        // Returns daily buckets — detailed SQL aggregation left for production; placeholder here
        Map<String, Object> result = new HashMap<>();
        result.put("days", days);
        result.put("series", java.util.List.of());
        result.put("note", "Connect orderRepository.dailyRevenue(from, to) for real data");
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> topProducts(int limit, int days) {
        Map<String, Object> result = new HashMap<>();
        var products = productRepository.findAll().stream()
                .filter(p -> p.isActive())
                .sorted((a, b) -> b.getSalesCount() - a.getSalesCount())
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
