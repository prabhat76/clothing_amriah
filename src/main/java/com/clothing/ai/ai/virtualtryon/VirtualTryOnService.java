package com.clothing.ai.ai.virtualtryon;

import com.clothing.ai.ai.client.OpenAiClient;
import com.clothing.ai.ai.virtualtryon.VirtualTryOnDtos.*;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.common.util.ImageStorageService;
import com.clothing.ai.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VirtualTryOnService {

    private final OpenAiClient openAi;
    private final ProductRepository productRepository;
    private final ImageStorageService imageStorage;
    private final AppProperties props;

    public TryOnResponse virtualTryOn(UUID productId, MultipartFile userImage, String userHeight) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","id",productId));
        if (userImage == null || userImage.isEmpty()) throw new BadRequestException("User image is required");
        if (product.getMainImageUrl() == null) throw new BadRequestException("Product has no image");

        String savedUrl = imageStorage.save(userImage, "tryon-uploads");

        String styleAdvice = "Try pairing with neutral basics to let this piece shine.";
        if (props.getAi().isEnabled()) {
            try {
                String prompt = String.format("""
                    The user is virtually trying on: %s.
                    Tags: %s. Category: %s.
                    Provide a 50-word styling tip explaining how this piece suits their silhouette,
                    and 2 accessory or footwear suggestions to complete the outfit.
                    """,
                    product.getName(), product.getTags(),
                    product.getCategory() != null ? product.getCategory().getName() : "apparel");
                String ai = openAi.chat(null, prompt);
                if (ai != null && !ai.isBlank()) styleAdvice = ai;
            } catch (Exception e) {
                log.warn("Style advice failed: {}", e.getMessage());
            }
        }

        return new TryOnResponse(
                UUID.randomUUID().toString(),
                savedUrl,
                product.getMainImageUrl(),
                styleAdvice,
                userHeight,
                0.78,
                props.getAi().isEnabled() ? "AI-assisted render" : "Static render (AI disabled)"
        );
    }

    public FitAnalysisResponse analyzeFit(UUID productId, MultipartFile userImage,
                                          Integer heightCm, Integer weightKg) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product","id",productId));
        String savedUrl = imageStorage.save(userImage, "fit-analysis");

        Double bmi = null;
        if (heightCm != null && heightCm > 0 && weightKg != null && weightKg > 0) {
            bmi = weightKg / Math.pow(heightCm / 100.0, 2);
        }
        String fitCategory = bmi == null ? "Regular" : bmi < 22 ? "Slim" : bmi < 27 ? "Regular" : bmi < 32 ? "Relaxed" : "Loose";
        return new FitAnalysisResponse(savedUrl, fitCategory, bmi,
                "Recommended fit type: " + fitCategory + ". Consider sizing up if between sizes.",
                product.getName());
    }
}
