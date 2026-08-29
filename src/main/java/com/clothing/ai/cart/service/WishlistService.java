package com.clothing.ai.cart.service;

import com.clothing.ai.cart.dto.CartDtos.WishlistResponse;
import com.clothing.ai.cart.entity.Wishlist;
import com.clothing.ai.cart.repository.WishlistRepository;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.ResourceNotFoundException;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Add product to wishlist. Idempotent. */
    @Transactional
    public void add(UUID userId, UUID productId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> wishlistRepository.save(
                        Wishlist.builder().user(u).product(p).build()));
    }

    /** Remove product from wishlist. Idempotent. */
    @Transactional
    public void remove(UUID userId, UUID productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /** List all wishlist items for a user. */
    @Transactional(readOnly = true)
    public List<WishlistResponse> list(UUID userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Returns true if the product is in the user's wishlist. */
    @Transactional(readOnly = true)
    public boolean isWishlisted(UUID userId, UUID productId) {
        return wishlistRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    // ------------------------------------------------------------------ helpers

    private WishlistResponse toResponse(Wishlist w) {
        Product p = w.getProduct();
        return new WishlistResponse(
                w.getId(), p.getId(), p.getName(), p.getSlug(),
                p.getMainImageUrl(), p.getPrice(), p.getAverageRating());
    }
}
