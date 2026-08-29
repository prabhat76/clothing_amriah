package com.clothing.ai.catalog.entity;

import com.clothing.ai.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brands")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Brand extends BaseEntity {
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(unique = true, length = 140)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(nullable = false)
    private boolean active = true;
}
