package com.clothing.ai.catalog.repository;

import com.clothing.ai.catalog.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BannerRepository extends JpaRepository<Banner, UUID> {

    List<Banner> findByActiveTrueAndDeletedFalseOrderByDisplayOrderAsc();

    List<Banner> findByDeletedFalseOrderByDisplayOrderAsc();
}
