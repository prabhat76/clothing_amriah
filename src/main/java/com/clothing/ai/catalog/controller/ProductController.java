package com.clothing.ai.catalog.controller;

import com.clothing.ai.catalog.dto.ProductDtos.*;
import com.clothing.ai.catalog.service.ProductService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Product catalogue endpoints.
 *
 * <p>Read endpoints are public. Write endpoints (create/update/delete) require
 * {@code ADMIN} or {@code STAFF} role.
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Product catalogue: list, search, detail, variants, and admin CRUD")
public class ProductController {

    private final ProductService productService;

    // ------------------------------------------------------------------ public reads

    @Operation(
            summary = "List all active products",
            description = """
                    Returns a paginated list of all active products, newest first.
                    Use `/products/search` for filtered queries.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Page of products returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid pagination parameters")
    })
    @SecurityRequirements   // public
    @GetMapping
    public ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @Parameter(description = "Zero-based page index", example = "0", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size (1–100)", example = "20", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Sort field and direction, e.g. `createdAt,desc`", in = ParameterIn.QUERY)
            @RequestParam(required = false) String sort) {
        return ApiResponse.success(PageResponse.from(
                productService.listProducts(PageRequest.of(page, size)).map(p -> p)));
    }

    @Operation(
            summary = "Get product by slug",
            description = "Returns full product detail — variants, tags, images — identified by the URL-friendly slug.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "No active product with this slug",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"success":false,"errorCode":"RESOURCE_NOT_FOUND",
                                     "message":"Product not found with slug: my-tshirt"}""")))
    })
    @SecurityRequirements
    @GetMapping("/{slug}")
    public ApiResponse<ProductDetailResponse> get(
            @Parameter(description = "Product slug, e.g. `classic-white-tee`") @PathVariable String slug) {
        return ApiResponse.success(productService.getBySlug(slug));
    }

    @Operation(
            summary = "Get product by internal UUID",
            description = "Admin-friendly alternative to the slug-based endpoint. Useful for linking from admin UIs.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @SecurityRequirements
    @GetMapping("/id/{id}")
    public ApiResponse<ProductDetailResponse> getById(
            @Parameter(description = "Product UUID") @PathVariable UUID id) {
        return ApiResponse.success(productService.getById(id));
    }

    @Operation(
            summary = "Search and filter products",
            description = """
                    Full-featured product search with optional filters.

                    | Parameter | Description |
                    |-----------|-------------|
                    | `q`       | Keyword search against name, description, tags |
                    | `categoryId` | Filter by category UUID |
                    | `brandId` | Filter by brand UUID |
                    | `minPrice` / `maxPrice` | Price range in the product's currency |
                    | `tag`     | Filter by a single tag slug |
                    | `sort`    | One of: `price,asc` `price,desc` `rating,desc` `createdAt,desc` |
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Search results"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid filter parameters")
    })
    @SecurityRequirements
    @GetMapping("/search")
    public ApiResponse<PageResponse<ProductSummaryResponse>> search(
            @Parameter(description = "Keyword search query") @RequestParam(required = false) String q,
            @Parameter(description = "Category UUID filter") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Brand UUID filter") @RequestParam(required = false) UUID brandId,
            @Parameter(description = "Minimum price (inclusive)") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price (inclusive)") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Tag slug filter") @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort) {
        return ApiResponse.success(PageResponse.from(
                productService.search(q, categoryId, brandId, minPrice, maxPrice, tag, page, size, sort)));
    }

    @Operation(
            summary = "List featured products",
            description = "Returns products flagged as `featured = true`, sorted by salesCount desc.")
    @SecurityRequirements
    @GetMapping("/featured")
    public ApiResponse<PageResponse<ProductSummaryResponse>> featured(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.success(PageResponse.from(
                productService.listProducts(PageRequest.of(page, size)).map(p -> p)));
    }

    @Operation(
            summary = "Record a product view",
            description = """
                    Increments the `viewCount` for analytics / trending calculation.
                    Call this after the product detail page loads. Fire-and-forget — always returns 200.
                    """)
    @SecurityRequirements
    @PostMapping("/{id}/view")
    public ApiResponse<Void> recordView(
            @Parameter(description = "Product UUID") @PathVariable UUID id) {
        productService.recordView(id);
        return ApiResponse.success(null);
    }

    // ------------------------------------------------------------------ admin writes

    @Operation(
            summary = "Create a new product [ADMIN/STAFF]",
            description = """
                    Creates a product in **DRAFT** status.
                    Activate it by setting `status: ACTIVE` with the update endpoint.

                    Requires role `ADMIN` or `STAFF`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error — missing required fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "SKU already exists")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<ProductDetailResponse> create(@Valid @RequestBody ProductCreateRequest req) {
        return ApiResponse.success("Product created",
                productService.createProduct(req, SecurityUtils.currentUserId().toString()));
    }

    @Operation(
            summary = "Update a product [ADMIN/STAFF]",
            description = """
                    Partial update — only non-null fields in the request body are applied.

                    Requires role `ADMIN` or `STAFF`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Product updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<ProductDetailResponse> update(
            @Parameter(description = "Product UUID") @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest req) {
        return ApiResponse.success("Product updated",
                productService.updateProduct(id, req, SecurityUtils.currentUserId().toString()));
    }

    @Operation(
            summary = "Archive (soft-delete) a product [ADMIN]",
            description = """
                    Sets `active = false` and `status = ARCHIVED`.
                    The product is no longer returned in public listings or search.

                    Requires role `ADMIN`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Product archived"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(
            @Parameter(description = "Product UUID") @PathVariable UUID id) {
        productService.deleteProduct(id, SecurityUtils.currentUserId().toString());
        return ApiResponse.success("Product archived", null);
    }

    @Operation(
            summary = "Add a variant to a product [ADMIN/STAFF]",
            description = """
                    Adds a size/colour/material variant to an existing product.
                    Each variant has its own SKU, stock count, and optional price override.

                    Requires role `ADMIN` or `STAFF`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Variant added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Variant SKU already exists")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ApiResponse<VariantResponse> addVariant(
            @Parameter(description = "Product UUID") @PathVariable UUID id,
            @Valid @RequestBody VariantCreateRequest req) {
        return ApiResponse.success("Variant added",
                productService.addVariant(id, req, SecurityUtils.currentUserId().toString()));
    }
}
