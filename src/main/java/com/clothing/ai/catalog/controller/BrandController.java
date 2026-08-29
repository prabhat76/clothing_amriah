package com.clothing.ai.catalog.controller;

import com.clothing.ai.catalog.dto.ProductDtos.*;
import com.clothing.ai.catalog.service.ProductService;
import com.clothing.ai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Brand management — public reads, admin writes.
 */
@RestController
@RequestMapping("/brands")
@RequiredArgsConstructor
@Tag(name = "Brands", description = "Brand management — public reads, admin writes")
public class BrandController {

    private final ProductService productService;

    @Operation(
            summary = "List all active brands",
            description = "Returns all brands with at least one active product.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Brand list returned")
    })
    @SecurityRequirements
    @GetMapping
    public ApiResponse<List<BrandResponse>> list() {
        return ApiResponse.success(productService.listBrands());
    }

    @Operation(
            summary = "Create a brand [ADMIN]",
            description = "Creates a new clothing brand entry. Requires role `ADMIN`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Brand created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Brand name already taken")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BrandResponse> create(@Valid @RequestBody BrandRequest req) {
        return ApiResponse.success("Brand created", productService.createBrand(req));
    }

    @Operation(
            summary = "Update a brand [ADMIN]",
            description = "Updates brand name, description, or logo. Requires role `ADMIN`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Brand updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Brand not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BrandResponse> update(
            @Parameter(description = "Brand UUID") @PathVariable UUID id,
            @Valid @RequestBody BrandRequest req) {
        return ApiResponse.success("Brand updated", productService.updateBrand(id, req));
    }

    @Operation(
            summary = "Delete a brand [ADMIN]",
            description = "Soft-deletes a brand. Fails if active products use this brand.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Brand deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Brand has active products — cannot delete")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(
            @Parameter(description = "Brand UUID") @PathVariable UUID id) {
        productService.deleteBrand(id);
        return ApiResponse.success("Brand deleted", null);
    }
}
