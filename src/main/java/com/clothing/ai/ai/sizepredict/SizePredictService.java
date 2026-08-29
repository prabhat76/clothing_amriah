package com.clothing.ai.ai.sizepredict;

import com.clothing.ai.ai.client.OpenAiClient;
import com.clothing.ai.ai.sizepredict.SizePredictDtos.*;
import com.clothing.ai.common.exception.BadRequestException;
import com.clothing.ai.config.AppProperties;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AI-assisted clothing size prediction.
 *
 * <p>Flow:
 * <ol>
 *   <li>If {@code userId} is provided, augment the request with stored profile measurements.</li>
 *   <li>Run the BMI size-chart algorithm as the baseline.</li>
 *   <li>If AI is enabled and a {@code preferredFit} is given, refine with GPT.</li>
 *   <li>Store measurements back to the user profile if they provided them.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SizePredictService {

    private final OpenAiClient openAi;
    private final AppProperties props;
    private final UserRepository userRepository;

    @Transactional
    public SizePrediction predict(UUID userId, SizePredictionRequest req) {
        // -------------------------------------------------------------- resolve measurements
        int height = req.heightCm() != null ? req.heightCm() : 0;
        int weight = req.weightKg() != null ? req.weightKg() : 0;
        String gender = req.gender() != null ? req.gender() : "UNISEX";

        // Pull from user profile when measurements are missing
        if (userId != null && (height == 0 || weight == 0)) {
            var userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                var u = userOpt.get();
                if (height == 0 && u.getHeightCm() != null) height = u.getHeightCm();
                if (weight == 0 && u.getWeightKg() != null) weight = u.getWeightKg();
                if ("UNISEX".equals(gender) && u.getGender() != null) {
                    gender = u.getGender().name(); // Gender enum → String
                }
            }
        }

        if (height < 100 || weight < 30) {
            throw new BadRequestException(
                    "Height and weight are required. Please provide them in the request or update your profile.");
        }

        // -------------------------------------------------------------- baseline: BMI chart
        SizePredictDtos.SizeChart.Row row = SizePredictDtos.SizeChart.findClosest(height, weight, gender);
        String rationale = "Based on height %d cm / weight %d kg (BMI %.1f) we recommend size %s for %s."
                .formatted(height, weight, weight / Math.pow(height / 100.0, 2), row.size(),
                        req.category() != null ? req.category() : "this category");

        SizePrediction prediction = new SizePrediction(row.size(), row.confidence(), "BMI_CHART", rationale);

        // -------------------------------------------------------------- AI refinement
        if (props.getAi().isEnabled() && req.preferredFit() != null) {
            try {
                String prompt = """
                        Customer measurements: height %d cm, weight %d kg, gender %s.
                        Preferred fit: %s. Garment category: %s.
                        Recommend ONE size (XS/S/M/L/XL/XXL) and explain in one sentence.
                        Reply format: <SIZE>: <reason>
                        """.formatted(height, weight, gender,
                        req.preferredFit(), req.category() != null ? req.category() : "tops");

                String aiResp = openAi.chat(null, prompt);
                if (aiResp != null && !aiResp.isBlank()) {
                    // Parse "M: Great choice because..." → size = "M"
                    String[] parts = aiResp.split(":", 2);
                    String aiSize = parts[0].trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                    String aiRationale = parts.length > 1 ? parts[1].trim() : aiResp;
                    if (!aiSize.isEmpty()) {
                        prediction = new SizePrediction(
                                aiSize,
                                Math.min(0.97, row.confidence() + 0.10),
                                "AI_MODEL",
                                aiRationale);
                    }
                }
            } catch (Exception e) {
                log.warn("AI size prediction failed, falling back to chart: {}", e.getMessage());
            }
        }

        // -------------------------------------------------------------- persist measurements to profile
        if (userId != null) {
            final int finalHeight = height;
            final int finalWeight = weight;
            final String finalGender = gender;
            userRepository.findById(userId).ifPresent(u -> {
                if (req.heightCm() != null) u.setHeightCm(finalHeight);
                if (req.weightKg() != null) u.setWeightKg(finalWeight);
                if (req.gender() != null) {
                    try {
                        u.setGender(com.clothing.ai.user.entity.User.Gender.valueOf(finalGender));
                    } catch (IllegalArgumentException ignored) { /* unknown gender string, skip */ }
                }
            });
        }

        return prediction;
    }
}
