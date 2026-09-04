package com.clothing.ai.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class BannerDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Banner as returned by the API")
    public static class BannerResponse {
        @Schema(description = "Banner UUID")
        private UUID id;
        @Schema(description = "Headline text")
        private String title;
        @Schema(description = "Sub-headline text")
        private String subtitle;
        @Schema(description = "Call-to-action button label")
        private String ctaText;
        @Schema(description = "Call-to-action link")
        private String ctaLink;
        @Schema(description = "Full image URL (Cloudinary or CDN)")
        private String imageUrl;
        @Schema(description = "Display order — lower is first")
        private int displayOrder;
        @Schema(description = "Whether the banner is visible on the storefront")
        private boolean active;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request body for creating a new banner")
    public static class CreateBannerRequest {
        @Schema(description = "Headline text", example = "Inspired by Jaipur. Designed for You.")
        @Size(max = 200)
        private String title;

        @Schema(description = "Sub-headline", example = "Explore the new collection")
        @Size(max = 500)
        private String subtitle;

        @Schema(description = "CTA button label", example = "Explore Collection")
        @Size(max = 100)
        private String ctaText;

        @Schema(description = "CTA link", example = "/shop")
        @Size(max = 500)
        private String ctaLink;

        @NotBlank
        @Schema(description = "Image URL", example = "https://res.cloudinary.com/astrimi/image/upload/v1/banner1.jpg")
        @Size(max = 500)
        private String imageUrl;

        @Schema(description = "Display order", example = "0")
        private int displayOrder = 0;

        @Schema(description = "Visible on storefront?", example = "true")
        private boolean active = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request body for updating an existing banner (all fields optional)")
    public static class UpdateBannerRequest {
        @Size(max = 200)
        private String title;
        @Size(max = 500)
        private String subtitle;
        @Size(max = 100)
        private String ctaText;
        @Size(max = 500)
        private String ctaLink;
        @Size(max = 500)
        private String imageUrl;
        private Integer displayOrder;
        private Boolean active;
    }
}
