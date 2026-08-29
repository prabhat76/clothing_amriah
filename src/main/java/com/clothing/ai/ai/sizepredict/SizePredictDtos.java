package com.clothing.ai.ai.sizepredict;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Size prediction DTOs.
 */
public class SizePredictDtos {

    @Schema(description = "Body measurements and preferences for size prediction")
    public record SizePredictionRequest(
            @Schema(description = "Height in centimetres (e.g. 175)", example = "175")
            @Min(100) @Max(250) Integer heightCm,

            @Schema(description = "Weight in kilograms (e.g. 70)", example = "70")
            @Min(30) @Max(300) Integer weightKg,

            @Schema(description = "Gender for size chart selection",
                    allowableValues = {"MALE", "FEMALE", "UNISEX"}, example = "MALE")
            String gender,

            @Schema(description = "Preferred fit style",
                    allowableValues = {"SLIM", "REGULAR", "RELAXED"}, example = "REGULAR")
            String preferredFit,

            @Schema(description = "Clothing category to predict size for",
                    allowableValues = {"tops", "bottoms", "dresses", "outerwear", "footwear"},
                    example = "tops")
            String category) {}

    @Schema(description = "Size prediction result")
    public record SizePrediction(
            @Schema(description = "Recommended size label", example = "M")
            String recommendedSize,

            @Schema(description = "Confidence score 0.0–1.0", example = "0.87")
            double confidence,

            @Schema(description = "Algorithm used", example = "BMI_CHART",
                    allowableValues = {"BMI_CHART", "AI_MODEL", "PROFILE_CACHE"})
            String method,

            @Schema(description = "Human-readable explanation of the recommendation")
            String rationale) {}

    // ------------------------------------------------------------------ internal

    static class SizeChart {
        record Row(String size, double confidence) {}

        static Row findClosest(int heightCm, int weightKg, String gender) {
            double bmi = weightKg / Math.pow(heightCm / 100.0, 2);
            // Apply gender adjustment: women typically size up by one in BMI thresholds
            boolean female = "FEMALE".equalsIgnoreCase(gender);
            if (bmi < (female ? 17.5 : 18.5)) return new Row("XS", 0.65);
            if (bmi < (female ? 21.0 : 22.0)) return new Row("S",  0.75);
            if (bmi < (female ? 26.0 : 27.0)) return new Row("M",  0.85);
            if (bmi < (female ? 31.0 : 32.0)) return new Row("L",  0.80);
            if (bmi < (female ? 36.0 : 37.0)) return new Row("XL", 0.75);
            return new Row("XXL", 0.70);
        }
    }
}
