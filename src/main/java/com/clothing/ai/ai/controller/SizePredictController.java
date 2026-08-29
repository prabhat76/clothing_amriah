package com.clothing.ai.ai.controller;

import com.clothing.ai.ai.sizepredict.SizePredictDtos.*;
import com.clothing.ai.ai.sizepredict.SizePredictService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI-powered clothing size prediction endpoints.
 */
@RestController
@RequestMapping("/ai/size-predict")
@RequiredArgsConstructor
@Tag(name = "AI - Size Predict",
        description = "AI-powered size recommendation based on body measurements and category")
public class SizePredictController {

    private final SizePredictService service;

    @Operation(
            summary = "Predict clothing size (public / guest)",
            description = """
                    Predicts the recommended size for a given clothing category based on body
                    measurements provided in the request body.

                    **Public endpoint** — authentication optional.
                    For authenticated users, see `/ai/size-predict/predict/me` which reads stored
                    measurements from the user's profile automatically.

                    Supported categories: `tops`, `bottoms`, `dresses`, `outerwear`, `footwear`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Size prediction returned",
                    content = @Content(schema = @Schema(implementation = SizePrediction.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "recommendedSize": "M",
                                        "confidence": 0.87,
                                        "method": "BMI_CHART",
                                        "rationale": "Based on height 175 cm / weight 72 kg, BMI 23.5 maps to size M for men's tops."
                                      }
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Height or weight out of valid range")
    })
    @SecurityRequirements
    @PostMapping("/predict")
    public ApiResponse<SizePrediction> predict(@Valid @RequestBody SizePredictionRequest req) {
        return ApiResponse.success(service.predict(null, req));
    }

    @Operation(
            summary = "Predict size using stored profile measurements",
            description = """
                    Same as `/predict` but pulls `heightCm`, `weightKg`, and `gender` from the
                    authenticated user's profile — the request body only needs `category` and optionally
                    `preferredFit` (`SLIM` | `REGULAR` | `RELAXED`).
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Size prediction returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "User profile missing height/weight — please update profile"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Not authenticated")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/predict/me")
    public ApiResponse<SizePrediction> predictFromProfile(@Valid @RequestBody SizePredictionRequest req) {
        return ApiResponse.success(service.predict(SecurityUtils.currentUserId(), req));
    }
}
