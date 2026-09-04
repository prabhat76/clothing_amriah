package com.clothing.ai.catalog.controller;

import com.clothing.ai.catalog.dto.BannerDtos;
import com.clothing.ai.catalog.service.BannerService;
import com.clothing.ai.common.response.ApiResponse;
import com.clothing.ai.common.util.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
@Tag(name = "Banners", description = "Homepage / hero carousel banners")
public class BannerController {

    private final BannerService bannerService;
    private final ImageStorageService imageStorageService;

    // ── Public ───────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List active banners", description = "Returns all active banners ordered by displayOrder. No auth required.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Banner list")
    })
    public ResponseEntity<ApiResponse<List<BannerDtos.BannerResponse>>> getActiveBanners() {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getActiveBanners()));
    }

    // ── Admin ────────────────────────────────────────────────────────────────

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "List all banners (admin)", description = "Returns every non-deleted banner, including inactive ones.")
    public ResponseEntity<ApiResponse<List<BannerDtos.BannerResponse>>> getAllBanners() {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getAllBanners()));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get banner by ID (admin)")
    public ResponseEntity<ApiResponse<BannerDtos.BannerResponse>> getById(
            @Parameter(description = "Banner UUID") @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(bannerService.getById(id)));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create banner", description = "Creates a new banner. imageUrl is required.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Banner created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ApiResponse<BannerDtos.BannerResponse>> create(
            @Valid @RequestBody BannerDtos.CreateBannerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Banner created", bannerService.create(req)));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update banner", description = "Partial update — only supplied fields are changed.")
    public ResponseEntity<ApiResponse<BannerDtos.BannerResponse>> update(
            @Parameter(description = "Banner UUID") @PathVariable UUID id,
            @Valid @RequestBody BannerDtos.UpdateBannerRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Banner updated", bannerService.update(id, req)));
    }

    @PostMapping(value = "/admin/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Upload banner image + create banner",
        description = "Accepts a multipart image file plus optional metadata. Stores the image and creates the banner record in one request."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Banner created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or invalid file")
    })
    public ResponseEntity<ApiResponse<BannerDtos.BannerResponse>> uploadAndCreate(
            @Parameter(description = "Banner image file (jpg/png/webp)")
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Headline text")
            @RequestParam(required = false) String title,
            @Parameter(description = "Sub-headline text")
            @RequestParam(required = false) String subtitle,
            @Parameter(description = "CTA button label", example = "Explore Collection")
            @RequestParam(required = false) String ctaText,
            @Parameter(description = "CTA link", example = "/shop")
            @RequestParam(required = false) String ctaLink,
            @Parameter(description = "Display order (0 = first)", example = "0")
            @RequestParam(defaultValue = "0") int displayOrder,
            @Parameter(description = "Make visible immediately?", example = "true")
            @RequestParam(defaultValue = "true") boolean active) {
        try {
            String imageUrl = imageStorageService.save(file, "banners");
            BannerDtos.CreateBannerRequest req = new BannerDtos.CreateBannerRequest(
                    title, subtitle, ctaText, ctaLink, imageUrl, displayOrder, active);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Banner created", bannerService.create(req)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UPLOAD_FAILED", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Delete banner (soft)", description = "Soft-deletes the banner. It will no longer be returned by the public endpoint.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Banner UUID") @PathVariable UUID id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
