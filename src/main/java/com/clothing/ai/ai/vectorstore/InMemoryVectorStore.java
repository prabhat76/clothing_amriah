package com.clothing.ai.ai.vectorstore;

import com.clothing.ai.ai.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory vector store backed by cosine similarity.
 * For production, replace with PGVector / Elasticsearch / Redis vector search.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryVectorStore {

    private final OpenAiClient openAi;
    private final Map<String, float[]> vectors = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> metadata = new ConcurrentHashMap<>();

    public void upsert(String id, String text, Map<String, String> meta) {
        float[] v = openAi.embed(text);
        if (v == null) return;
        vectors.put(id, v);
        metadata.put(id, meta != null ? meta : Map.of());
        log.debug("Vector indexed: {} (dim={})", id, v.length);
    }

    public List<Match> similaritySearch(String query, int topK) {
        float[] qv = openAi.embed(query);
        if (qv == null) return List.of();
        return vectors.entrySet().stream()
                .map(e -> new Match(e.getKey(), cosine(qv, e.getValue())))
                .sorted(Comparator.comparingDouble(Match::score).reversed())
                .limit(topK)
                .toList();
    }

    public Map<String, String> metadata(String id) {
        return metadata.getOrDefault(id, Map.of());
    }

    public int size() { return vectors.size(); }

    private double cosine(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public record Match(String id, double score) {}
}
