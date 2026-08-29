package com.clothing.ai.ai.descriptions;

import com.clothing.ai.ai.client.OpenAiClient;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDescriptionService {

    private final OpenAiClient openAi;
    private final AppProperties props;

    @Cacheable(value = "ai-cache", key = "'desc-' + #product.id + '-' + #product.name")
    public String generateDescription(Product product) {
        if (!props.getAi().isEnabled()) return defaultDescription(product);
        String prompt = String.format("""
            You are an expert fashion copywriter. Write an engaging, 80-120 word product description
            for a clothing e-commerce listing.

            Product name: %s
            Brand: %s
            Category: %s
            Tags: %s
            Existing short description: %s

            Requirements:
            - Conversational, vivid, sensory language
            - Highlight style, fabric, fit, occasion, and care
            - Avoid hard-sell language
            - Use 1-2 tasteful emojis max
            - Output plain text only, no headings
            """,
                product.getName(),
                product.getBrand() != null ? product.getBrand().getName() : "Generic",
                product.getCategory() != null ? product.getCategory().getName() : "Clothing",
                product.getTags(),
                product.getShortDescription() != null ? product.getShortDescription() : ""
        );
        String out = openAi.chat(null, prompt);
        return out != null ? out : defaultDescription(product);
    }

    public String generateShortDescription(Product product) {
        if (!props.getAi().isEnabled()) return product.getShortDescription();
        String prompt = "Write a 1-sentence, max 25-word punchy product tagline for: " + product.getName()
                + ". Tags: " + product.getTags();
        String out = openAi.chat(null, prompt);
        return out != null ? out : defaultDescription(product);
    }

    public String defaultDescription(Product product) {
        return "Discover the " + product.getName() + " — a versatile addition to your wardrobe. "
                + "Crafted with attention to detail, this piece combines comfort, style, and quality. "
                + "Perfect for everyday wear or special occasions. Available in multiple sizes and colors.";
    }
}
