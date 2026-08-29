package com.clothing.ai.catalog.controller;

import com.clothing.ai.catalog.dto.ProductDtos.*;
import com.clothing.ai.catalog.service.ProductService;
import com.clothing.ai.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Category tree management — public reads, admin writes.
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category tree — nested categories with optional parent")
public class CategoryController {

    private final ProductService productService;

    @Operation(
            summary = "List all active categories",
            description = """
                    Returns the flat list of all active categories.
                    Use `parentId` to reconstruct the hierarchy on the client.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Category list returned",
                    content = @Content(schema = @Schema(implementation = CategoryResponse.class)))
    })
    @SecurityRequirements
    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success(productService.listCategories());
    }

    @Operation(
            summary = "Create a category [ADMIN]",
            description = """
                    Creates a new product category.
                    Set `parentId` to make it a sub-category.

                    Requires role `ADMIN`.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Parent category not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Category name already taken")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        return ApiResponse.success("Category created", productService.createCategory(req));
    }

    @Operation(
            summary = "Update a category [ADMIN]",
            description = "Updates name, description, image, display order, or parent of a category.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Category updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error or circular parent reference"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Category not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @Parameter(description = "Category UUID") @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest req) {
        return ApiResponse.success("Category updated", productService.updateCategory(id, req));
    }

    @Operation(
            summary = "Delete a category [ADMIN]",
            description = """
                    Soft-deletes a category (sets `active = false`).
                    Fails if the category has active products.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Category deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Category has active products — cannot delete"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Category not found")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(
            @Parameter(description = "Category UUID") @PathVariable UUID id) {
        productService.deleteCategory(id);
        return ApiResponse.success("Category deleted", null);
    }
}
