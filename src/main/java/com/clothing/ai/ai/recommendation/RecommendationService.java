package com.clothing.ai.ai.recommendation;

import com.clothing.ai.ai.vectorstore.InMemoryVectorStore;
import com.clothing.ai.cart.repository.WishlistRepository;
import com.clothing.ai.catalog.dto.ProductDtos.ProductSummaryResponse;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.mapper.ProductMapper;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.ai.client.OpenAiClient;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductMapper productMapper;
    private final AppProperties props;
    private final OpenAiClient openAi;
    private final InMemoryVectorStore vectorStore;

    public List<ProductSummaryResponse> similarProducts(UUID productId, int limit) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","id",productId));
        String tag = p.getTags().stream().findFirst().orElse(p.getCategory().getName());
        return productRepository.search(null,
                p.getCategory() != null ? p.getCategory().getId() : null,
                null, null, null, tag,
                PageRequest.of(0, limit + 1))
                .map(productMapper::toSummary)
                .getContent().stream()
                .filter(r -> !r.id().equals(productId))
                .limit(limit)
                .toList();
    }

    @Cacheable(value = "recommendations", key = "'personal-' + #userId + '-' + #limit")
    public List<ProductSummaryResponse> personalized(UUID userId, int limit) {
        Set<UUID> purchased = orderRepository.findByUserId(userId, PageRequest.of(0, 50))
                .getContent().stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getVariant().getProduct().getId())
                .collect(Collectors.toSet());

        Set<UUID> wishlisted = wishlistRepository.findByUserId(userId, PageRequest.of(0, 50))
                .getContent().stream().map(w -> w.getProduct().getId()).collect(Collectors.toSet());

        Set<String> tags = new HashSet<>();
        purchased.forEach(id -> productRepository.findById(id).ifPresent(pr -> tags.addAll(pr.getTags())));
        wishlisted.forEach(id -> productRepository.findById(id).ifPresent(pr -> tags.addAll(pr.getTags())));

        if (tags.isEmpty()) return trending(limit);

        return productRepository.search(null, null, null, null, null, tags.iterator().next(),
                PageRequest.of(0, limit * 2))
                .map(productMapper::toSummary)
                .getContent().stream()
                .filter(p -> !purchased.contains(p.id()) && !wishlisted.contains(p.id()))
                .limit(limit)
                .toList();
    }

    @Cacheable(value = "recommendations", key = "'trending-' + #limit")
    public List<ProductSummaryResponse> trending(int limit) {
        return productRepository.findTop12ByActiveTrueOrderBySalesCountDesc().stream()
                .limit(limit).map(productMapper::toSummary).toList();
    }

    @Cacheable(value = "recommendations", key = "'ai-' + #userId + '-' + #limit")
    public List<ProductSummaryResponse> aiPersonalized(UUID userId, int limit) {
        if (!props.getAi().isEnabled()) return personalized(userId, limit);
        try {
            String prompt = buildAiPrompt(userId);
            String aiResp = openAi.chat(null, prompt);
            if (aiResp == null) return personalized(userId, limit);
            return parseRecommendations(aiResp, limit);
        } catch (Exception e) {
            log.warn("AI recommendation failed, falling back to personalized: {}", e.getMessage());
            return personalized(userId, limit);
        }
    }

    @Async("taskExecutor")
    public void indexProductAsync(Product product) {
        try {
            String text = product.getName() + " " + product.getShortDescription() + " " + product.getDescription()
                    + " " + product.getTags();
            vectorStore.upsert(product.getId().toString(), text,
                    Map.of("sku", product.getSku(),
                           "name", product.getName(),
                           "category", product.getCategory() != null ? product.getCategory().getName() : ""));
            log.debug("Indexed product {} into vector store", product.getId());
        } catch (Exception e) {
            log.warn("Vector index failed for {}: {}", product.getId(), e.getMessage());
        }
    }

    public List<ProductSummaryResponse> semanticSearch(String query, int limit) {
        try {
            List<InMemoryVectorStore.Match> matches = vectorStore.similaritySearch(query, limit);
            List<ProductSummaryResponse> out = new ArrayList<>();
            for (var m : matches) {
                try {
                    UUID id = UUID.fromString(m.id());
                    Product p = productRepository.findById(id).orElse(null);
                    if (p != null && p.isActive()) out.add(productMapper.toSummary(p));
                } catch (Exception ignored) {}
            }
            return out;
        } catch (Exception e) {
            log.warn("Semantic search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildAiPrompt(UUID userId) {
        var recentOrders = orderRepository.findByUserId(userId, PageRequest.of(0, 5))
                .getContent().stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getProductName() + " (sku: " + i.getSku() + ")")
                .toList();
        var wishlist = wishlistRepository.findByUserId(userId, PageRequest.of(0, 10))
                .getContent().stream().map(w -> w.getProduct().getName()).toList();
        return """
            Based on the user's recent purchases: %s
            And their wishlist: %s
            Recommend up to %d product SKUs from our catalog that match their taste.
            Reply with only a JSON array of SKU strings, e.g. ["CL-001","CL-002"].
            """.formatted(recentOrders, wishlist, 10);
    }

    private List<ProductSummaryResponse> parseRecommendations(String aiResp, int limit) {
        try {
            String json = aiResp.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = json.indexOf("[");
            int end = json.lastIndexOf("]");
            if (start < 0 || end < 0) return trending(limit);
            String[] skus = json.substring(start, end + 1).replace("[", "").replace("]", "")
                    .replace("\"", "").split(",");
            List<ProductSummaryResponse> out = new ArrayList<>();
            for (String s : skus) {
                String sku = s.trim();
                if (sku.isEmpty()) continue;
                productRepository.findBySlug(sku).ifPresent(p -> out.add(productMapper.toSummary(p)));
                if (out.size() >= limit) break;
            }
            return out.isEmpty() ? trending(limit) : out;
        } catch (Exception e) {
            return trending(limit);
        }
    }
}
