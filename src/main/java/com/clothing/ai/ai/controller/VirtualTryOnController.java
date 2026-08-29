package com.clothing.ai.ai.controller;

import com.clothing.ai.ai.virtualtryon.VirtualTryOnDtos.*;
import com.clothing.ai.ai.virtualtryon.VirtualTryOnService;
import com.clothing.ai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * AI-powered virtual try-on and fit analysis endpoints.
 */
@RestController
@RequestMapping("/ai/virtual-try-on")
@RequiredArgsConstructor
@Tag(name = "AI - Virtual Try-On",
        description = "Upload a photo for AI style advice, virtual try-on overlay, and fit analysis")
@SecurityRequirement(name = "BearerAuth")
public class VirtualTryOnController {

    private final VirtualTryOnService service;

    @Operation(
            summary = "Virtual try-on style advice",
            description = """
                    Upload a user photo and receive AI-generated style advice and compatibility
                    assessment for the specified product.

                    **Request:** `multipart/form-data`
                    - `image` — JPEG/PNG/WebP, max 10 MB
                    - `userHeight` — optional height string, e.g. `"175cm"` or `"5ft9"`

                    **Response** includes:
                    - AI style commentary for the product + customer photo
                    - Suggested complementary products
                    - Occasion recommendations

                    _Note: actual garment overlay (AR try-on) requires the optional AR SDK integration._
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Style advice returned",
                    content = @Content(schema = @Schema(implementation = TryOnResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Image too large, invalid format, or product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "AI vision service unavailable")
    })
    @PostMapping(value = "/try/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TryOnResponse> tryOn(
            @Parameter(description = "Product UUID") @PathVariable UUID productId,
            @Parameter(description = "User photo (JPEG/PNG/WebP, max 10 MB)", required = true)
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "User height hint, e.g. 175cm")
            @RequestParam(required = false) String userHeight) throws IOException {

        validateImage(image);
        return ApiResponse.success(service.virtualTryOn(productId, image, userHeight));
    }

    @Operation(
            summary = "AI fit analysis",
            description = """
                    Analyse how a specific product will fit based on a photo and measurements.

                    Returns:
                    - Fit rating (SLIM / TRUE_TO_SIZE / OVERSIZED)
                    - Recommended size
                    - Alteration suggestions
                    - Confidence score
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Fit analysis returned",
                    content = @Content(schema = @Schema(implementation = FitAnalysisResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid image or measurements out of valid range"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @PostMapping(value = "/fit-analysis/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FitAnalysisResponse> fitAnalysis(
            @Parameter(description = "Product UUID") @PathVariable UUID productId,
            @Parameter(description = "User photo", required = true)
            @RequestParam("image") MultipartFile image,
            @Parameter(description = "User height in cm (optional)") @RequestParam(required = false) Integer heightCm,
            @Parameter(description = "User weight in kg (optional)") @RequestParam(required = false) Integer weightKg)
            throws IOException {

        validateImage(image);
        return ApiResponse.success(service.analyzeFit(productId, image, heightCm, weightKg));
    }

    // -------------------------------------------------------------- helpers

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new com.clothing.ai.common.exception.BadRequestException("Image file is required");
        }
        long maxBytes = 10 * 1024 * 1024L; // 10 MB
        if (image.getSize() > maxBytes) {
            throw new com.clothing.ai.common.exception.BadRequestException(
                    "Image size %d bytes exceeds the 10 MB limit".formatted(image.getSize()));
        }
        String ct = image.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new com.clothing.ai.common.exception.BadRequestException(
                    "Invalid content type '%s' — only image/* is accepted".formatted(ct));
        }
    }
}
