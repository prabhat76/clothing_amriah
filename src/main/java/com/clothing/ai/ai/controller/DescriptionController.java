package com.clothing.ai.ai.controller;

import com.clothing.ai.ai.descriptions.AiDescriptionService;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.ResourceNotFoundException;
import com.clothing.ai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * AI-generated product description management.
 */
@RestController
@RequestMapping("/ai/descriptions")
@RequiredArgsConstructor
@Tag(name = "AI - Descriptions",
        description = "Admin-only: generate and manage AI-written product descriptions (results are cached)")
@SecurityRequirement(name = "BearerAuth")
public class DescriptionController {

    private final AiDescriptionService service;
    private final ProductRepository productRepository;

    @Operation(
            summary = "Generate AI description for a product [ADMIN/STAFF]",
            description = """
                    Uses GPT-4o-mini to generate a professional e-commerce product description
                    based on product metadata (name, category, tags, existing description).

                    **Caching:** generated descriptions are stored on the product and returned
                    from cache on subsequent calls.  Pass `?force=true` to regenerate even if
                    a cached description exists.

                    Requires role `ADMIN` or `STAFF`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "AI description generated and saved",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": "Crafted from premium 100% organic cotton, the Classic White Tee is…"
                                    }"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503", description = "OpenAI service unavailable")
    })
    @PostMapping("/generate/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<String> generate(
            @Parameter(description = "Product UUID") @PathVariable UUID productId,
            @Parameter(description = "Force regenerate even if a cached description exists")
            @RequestParam(defaultValue = "false") boolean force) {

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (!force && p.getAiGeneratedDescription() != null && !p.getAiGeneratedDescription().isBlank()) {
            return ApiResponse.success("Cached description returned", p.getAiGeneratedDescription());
        }

        String description = service.generateDescription(p);
        p.setAiGeneratedDescription(description);
        productRepository.save(p);
        return ApiResponse.success("AI description generated", description);
    }

    @Operation(
            summary = "Clear AI description for a product [ADMIN]",
            description = "Removes the cached AI-generated description so the next call regenerates it.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Description cleared"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> clear(
            @Parameter(description = "Product UUID") @PathVariable UUID productId) {

        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        p.setAiGeneratedDescription(null);
        productRepository.save(p);
        return ApiResponse.success("AI description cleared", null);
    }
}
