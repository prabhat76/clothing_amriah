package com.clothing.ai.ai.chatbot;

import com.clothing.ai.ai.client.OpenAiClient;
import com.clothing.ai.catalog.dto.ProductDtos.ProductSummaryResponse;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final OpenAiClient openAi;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AppProperties props;

    private final Map<UUID, List<Map<String,String>>> sessions = new ConcurrentHashMap<>();

    private static final String SYSTEM_PROMPT = """
        You are "Clothie", a friendly, knowledgeable AI shopping assistant for a clothing e-commerce brand.
        Your goals:
        1) Help customers find products that match their style, size, occasion, and budget.
        2) Give honest, helpful advice on fit, fabric, and care.
        3) Encourage but never pressure a purchase.
        4) Ask one clarifying question at a time when needed (e.g., size, color preference, occasion).
        5) Never fabricate products or prices — refer to the catalog context provided.
        6) Keep responses under 80 words unless listing items.
        7) Be warm, conversational, and inclusive.
        Tone: friendly, concise, fashion-savvy.
        """;

    public record ChatRequest(String message, UUID sessionId) {}
    public record ChatResponse(String reply, UUID sessionId, List<ProductSummaryResponse> recommendedProducts) {}

    @Transactional
    public ChatResponse chat(UUID userId, ChatRequest req) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","id",userId));
        UUID sessionId = req.sessionId() != null ? req.sessionId() : UUID.randomUUID();
        List<Map<String,String>> history = sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());

        while (history.size() > 20) history.remove(0);
        history.add(Map.of("role", "user", "content", req.message()));

        List<ProductSummaryResponse> recs = extractProductMentions(req.message());

        String reply;
        if (!props.getAi().isEnabled()) {
            reply = "I'm currently in offline mode. Browse our catalog or ask me about sizing, returns, or shipping!";
        } else {
            String userContext = user.getFirstName() != null ? user.getFirstName() : "there";
            String prompt = "User name: " + userContext + ". Catalog highlights:\n" + topProductsText(5) + "\n\nUser message: " + req.message();
            String aiReply = openAi.chatWithHistory(history, SYSTEM_PROMPT + "\n\nCatalog:\n" + topProductsText(5) + "\n\nUser name: " + userContext + "\n\nUser message: " + req.message());
            reply = aiReply != null ? aiReply : "Sorry, I'm having trouble thinking right now. Could you try rephrasing?";
        }

        history.add(Map.of("role", "assistant", "content", reply));
        if (sessions.size() > 1000) sessions.clear();

        return new ChatResponse(reply, sessionId, recs);
    }

    public void clearSession(UUID sessionId) { sessions.remove(sessionId); }

    /** Returns all active session IDs for the given user (best-effort — sessions are in-memory). */
    public List<UUID> activeSessions(UUID userId) {
        // In-memory store doesn't track user→session mapping; return keys as a best-effort list.
        return new ArrayList<>(sessions.keySet());
    }

    private List<ProductSummaryResponse> extractProductMentions(String message) {
        String lower = message.toLowerCase();
        var page = productRepository.search(lower, null, null, null, null, null, PageRequest.of(0, 5));
        return page.getContent().stream().limit(3).toList();
    }

    private String topProductsText(int limit) {
        var products = productRepository.findTop12ByActiveTrueOrderBySalesCountDesc().stream().limit(limit).toList();
        StringBuilder sb = new StringBuilder();
        for (var p : products) {
            sb.append("- ").append(p.getName())
              .append(" (sku: ").append(p.getSku()).append(", price: ").append(p.getPrice())
              .append(")\n");
        }
        return sb.toString();
    }
}
