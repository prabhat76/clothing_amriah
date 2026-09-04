package com.clothing.ai.catalog.service;

import com.clothing.ai.catalog.dto.BannerDtos;
import com.clothing.ai.catalog.entity.Banner;
import com.clothing.ai.catalog.repository.BannerRepository;
import com.clothing.ai.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    // ── Public: only active, ordered ────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BannerDtos.BannerResponse> getActiveBanners() {
        return bannerRepository
                .findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Admin: all (including inactive) ─────────────────────────────────────
    @Transactional(readOnly = true)
    public List<BannerDtos.BannerResponse> getAllBanners() {
        return bannerRepository
                .findByDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BannerDtos.BannerResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public BannerDtos.BannerResponse create(BannerDtos.CreateBannerRequest req) {
        Banner banner = Banner.builder()
                .title(req.getTitle())
                .subtitle(req.getSubtitle())
                .ctaText(req.getCtaText())
                .ctaLink(req.getCtaLink())
                .imageUrl(req.getImageUrl())
                .displayOrder(req.getDisplayOrder())
                .active(req.isActive())
                .build();
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerDtos.BannerResponse update(UUID id, BannerDtos.UpdateBannerRequest req) {
        Banner banner = findOrThrow(id);
        if (req.getTitle() != null)        banner.setTitle(req.getTitle());
        if (req.getSubtitle() != null)     banner.setSubtitle(req.getSubtitle());
        if (req.getCtaText() != null)      banner.setCtaText(req.getCtaText());
        if (req.getCtaLink() != null)      banner.setCtaLink(req.getCtaLink());
        if (req.getImageUrl() != null)     banner.setImageUrl(req.getImageUrl());
        if (req.getDisplayOrder() != null) banner.setDisplayOrder(req.getDisplayOrder());
        if (req.getActive() != null)       banner.setActive(req.getActive());
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(UUID id) {
        Banner banner = findOrThrow(id);
        banner.setDeleted(true);
        bannerRepository.save(banner);
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private Banner findOrThrow(UUID id) {
        return bannerRepository.findById(id)
                .filter(b -> !b.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Banner", "id", id));
    }

    private BannerDtos.BannerResponse toResponse(Banner b) {
        return BannerDtos.BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .subtitle(b.getSubtitle())
                .ctaText(b.getCtaText())
                .ctaLink(b.getCtaLink())
                .imageUrl(b.getImageUrl())
                .displayOrder(b.getDisplayOrder())
                .active(b.isActive())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
