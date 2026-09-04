package com.clothing.ai.catalog.entity;

import com.clothing.ai.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Banner extends BaseEntity {

    @Column(length = 200)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "cta_text", length = 100)
    private String ctaText;

    @Column(name = "cta_link", length = 500)
    private String ctaLink;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
