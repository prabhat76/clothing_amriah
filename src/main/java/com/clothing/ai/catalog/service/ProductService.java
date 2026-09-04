package com.clothing.ai.catalog.service;

import com.clothing.ai.ai.descriptions.AiDescriptionService;
import com.clothing.ai.audit.service.AuditService;
import com.clothing.ai.catalog.dto.ProductDtos.*;
import com.clothing.ai.catalog.entity.*;
import com.clothing.ai.catalog.entity.Product.ProductStatus;
import com.clothing.ai.catalog.mapper.ProductMapper;
import com.clothing.ai.catalog.repository.*;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.common.util.SlugUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final AiDescriptionService aiDescriptionService;
    private final ProductMapper productMapper;
    private final AuditService auditService;

    // ===== Categories =====
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(productMapper::toCategoryResponse).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        Category c = Category.builder()
                .name(req.name()).slug(SlugUtil.slug(req.name()))
                .description(req.description()).imageUrl(req.imageUrl())
                .displayOrder(req.displayOrder() != null ? req.displayOrder() : 0)
                .active(true).build();
        if (req.parentId() != null) {
            c.setParent(categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category","id",req.parentId())));
        }
        return productMapper.toCategoryResponse(categoryRepository.save(c));
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest req) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category","id",id));
        if (req.name() != null && !req.name().isBlank()) {
            c.setName(req.name());
            c.setSlug(SlugUtil.slug(req.name()));
        }
        if (req.description() != null) c.setDescription(req.description());
        if (req.imageUrl() != null) c.setImageUrl(req.imageUrl());
        if (req.displayOrder() != null) c.setDisplayOrder(req.displayOrder());
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) throw new BadRequestException("A category cannot be its own parent");
            c.setParent(categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category","id",req.parentId())));
        }
        return productMapper.toCategoryResponse(categoryRepository.save(c));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category","id",id));
        long activeProducts = productRepository.countByCategoryIdAndActiveTrue(id);
        if (activeProducts > 0) {
            throw new ConflictException("Category has " + activeProducts + " active products — reassign or archive them first");
        }
        c.setActive(false);
    }

    // ===== Brands =====
    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        return brandRepository.findAll().stream().map(productMapper::toBrandResponse).toList();
    }

    @Transactional
    public BrandResponse createBrand(BrandRequest req) {
        Brand b = Brand.builder()
                .name(req.name()).slug(SlugUtil.slug(req.name()))
                .description(req.description()).logoUrl(req.logoUrl()).active(true).build();
        return productMapper.toBrandResponse(brandRepository.save(b));
    }

    @Transactional
    public BrandResponse updateBrand(UUID id, BrandRequest req) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand","id",id));
        if (req.name() != null && !req.name().isBlank()) {
            b.setName(req.name());
            b.setSlug(SlugUtil.slug(req.name()));
        }
        if (req.description() != null) b.setDescription(req.description());
        if (req.logoUrl() != null) b.setLogoUrl(req.logoUrl());
        return productMapper.toBrandResponse(brandRepository.save(b));
    }

    @Transactional
    public void deleteBrand(UUID id) {
        Brand b = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand","id",id));
        long activeProducts = productRepository.countByBrandIdAndActiveTrue(id);
        if (activeProducts > 0) {
            throw new ConflictException("Brand has " + activeProducts + " active products — reassign them first");
        }
        b.setActive(false);
    }

    // ===== Products =====
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<ProductSummaryResponse> listProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable).map(productMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        Product p = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product","slug",slug));
        return productMapper.toDetail(p);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getById(UUID id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product","id",id));
        return productMapper.toDetail(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> search(String q, UUID categoryId, UUID brandId, BigDecimal minPrice,
                                               BigDecimal maxPrice, String tag, int page, int size, String sort) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        return productRepository.search(q, categoryId, brandId, minPrice, maxPrice, tag, pageable)
                .map(productMapper::toSummary);
    }

    @Transactional
    public ProductDetailResponse createProduct(ProductCreateRequest req, String actorId) {
        Product p = Product.builder()
                .name(req.name()).slug(SlugUtil.slug(req.name()) + "-" + UUID.randomUUID().toString().substring(0,6))
                .sku(req.sku()).description(req.description()).shortDescription(req.shortDescription())
                .price(req.price()).compareAtPrice(req.compareAtPrice()).costPrice(req.costPrice())
                .currency(req.currency() != null ? req.currency() : "USD")
                .mainImageUrl(req.mainImageUrl()).imageUrls(req.imageUrls() != null ? req.imageUrls() : new HashSet<>())
                .tags(req.tags() != null ? req.tags() : new HashSet<>())
                .category(categoryRepository.findById(req.categoryId())
                        .orElseThrow(() -> new ResourceNotFoundException("Category","id",req.categoryId())))
                .brand(req.brandId() != null ? brandRepository.findById(req.brandId()).orElse(null) : null)
                .status(ProductStatus.ACTIVE).active(true)
                .featured(req.featured()).newArrival(req.newArrival())
                .weightGrams(req.weightGrams()).build();
        p = productRepository.save(p);

        if (req.variants() != null) {
            for (var v : req.variants()) {
                variantRepository.save(ProductVariant.builder()
                        .product(p).sku(v.sku()).size(v.size()).color(v.color()).colorHex(v.colorHex())
                        .material(v.material()).price(v.price()).salePrice(v.salePrice())
                        .stockQuantity(v.stockQuantity()).lowStockThreshold(v.lowStockThreshold() != null ? v.lowStockThreshold() : 5)
                        .imageUrl(v.imageUrl()).barcode(v.barcode()).active(true).build());
            }
        }
        auditService.record(actorId, "PRODUCT_CREATE", "Product", p.getId().toString(),
                "Created product " + p.getName());
        return productMapper.toDetail(p);
    }

    @Transactional
    public ProductDetailResponse updateProduct(UUID id, ProductUpdateRequest req, String actorId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product","id",id));
        if (req.name() != null) p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.shortDescription() != null) p.setShortDescription(req.shortDescription());
        if (req.categoryId() != null) p.setCategory(categoryRepository.findById(req.categoryId()).orElseThrow());
        if (req.brandId() != null) p.setBrand(brandRepository.findById(req.brandId()).orElse(null));
        if (req.price() != null) p.setPrice(req.price());
        if (req.compareAtPrice() != null) p.setCompareAtPrice(req.compareAtPrice());
        if (req.costPrice() != null) p.setCostPrice(req.costPrice());
        if (req.mainImageUrl() != null) p.setMainImageUrl(req.mainImageUrl());
        if (req.imageUrls() != null) p.setImageUrls(req.imageUrls());
        if (req.tags() != null) p.setTags(req.tags());
        if (req.featured() != null) p.setFeatured(req.featured());
        if (req.newArrival() != null) p.setNewArrival(req.newArrival());
        if (req.weightGrams() != null) p.setWeightGrams(req.weightGrams());
        if (req.active() != null) p.setActive(req.active());
        if (req.status() != null) p.setStatus(req.status());
        auditService.record(actorId, "PRODUCT_UPDATE", "Product", id.toString(),
                "Updated product " + p.getName());
        return productMapper.toDetail(p);
    }

    /**
     * Attach an already-uploaded image URL to a product.
     * If {@code setAsMain} is true, replaces {@code mainImageUrl}; otherwise appends to {@code imageUrls}.
     */
    @Transactional
    public ProductDetailResponse addProductImage(UUID productId, String imageUrl, boolean setAsMain, String actorId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        if (setAsMain) {
            p.setMainImageUrl(imageUrl);
        } else {
            Set<String> images = p.getImageUrls() != null ? new HashSet<>(p.getImageUrls()) : new HashSet<>();
            images.add(imageUrl);
            p.setImageUrls(images);
        }
        auditService.record(actorId, "PRODUCT_IMAGE_ADD", "Product", productId.toString(),
                "Added image " + imageUrl);
        return productMapper.toDetail(p);
    }

    @Transactional
    public void deleteProduct(UUID id, String actorId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product","id",id));
        p.setActive(false);
        p.setStatus(ProductStatus.ARCHIVED);
        auditService.record(actorId, "PRODUCT_DELETE", "Product", id.toString(), "Archived product");
    }

    @Transactional
    public VariantResponse addVariant(UUID productId, VariantCreateRequest req, String actorId) {
        Product p = productRepository.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product","id",productId));
        ProductVariant v = ProductVariant.builder()
                .product(p).sku(req.sku()).size(req.size()).color(req.color()).colorHex(req.colorHex())
                .material(req.material()).price(req.price()).salePrice(req.salePrice())
                .stockQuantity(req.stockQuantity()).lowStockThreshold(req.lowStockThreshold() != null ? req.lowStockThreshold() : 5)
                .imageUrl(req.imageUrl()).barcode(req.barcode()).active(true).build();
        v = variantRepository.save(v);
        auditService.record(actorId, "VARIANT_CREATE", "ProductVariant", v.getId().toString(),
                "Added variant " + v.getSku());
        return productMapper.toVariantResponse(v);
    }

    @Transactional
    public void recordView(UUID productId) {
        productRepository.findById(productId).ifPresent(p -> p.setViewCount(p.getViewCount() + 1));
    }

    @Transactional
    public void decrementStock(UUID variantId, int qty) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant","id",variantId));
        if (v.getStockQuantity() < qty) throw new BadRequestException("Insufficient stock for SKU " + v.getSku());
        v.setStockQuantity(v.getStockQuantity() - qty);
        Product p = v.getProduct();
        p.setSalesCount(p.getSalesCount() + qty);
    }

    private Sort parseSort(String s) {
        if (s == null || s.isBlank()) return Sort.by("createdAt").descending();
        return switch (s) {
            case "price-asc" -> Sort.by("price").ascending();
            case "price-desc" -> Sort.by("price").descending();
            case "name" -> Sort.by("name").ascending();
            case "rating" -> Sort.by("averageRating").descending();
            case "popular" -> Sort.by("salesCount").descending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
