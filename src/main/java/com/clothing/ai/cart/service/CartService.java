package com.clothing.ai.cart.service;

import com.clothing.ai.cart.dto.CartDtos.*;
import com.clothing.ai.cart.entity.Cart;
import com.clothing.ai.cart.entity.CartItem;
import com.clothing.ai.cart.repository.CartItemRepository;
import com.clothing.ai.cart.repository.CartRepository;
import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.entity.ProductVariant;
import com.clothing.ai.catalog.repository.ProductVariantRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal SHIPPING_FLAT = new BigDecimal("9.99");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("100.00");

    @Transactional
    public CartResponse getOrCreateCart(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User","id",userId));
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart c = Cart.builder().user(user).items(new ArrayList<>()).build();
            return cartRepository.save(c);
        });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID userId, AddToCartRequest req) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            User u = userRepository.findById(userId).orElseThrow();
            Cart c = Cart.builder().user(u).items(new ArrayList<>()).build();
            return cartRepository.save(c);
        });
        ProductVariant variant = variantRepository.findById(req.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant","id",req.variantId()));
        if (variant.getStockQuantity() < req.quantity())
            throw new BadRequestException("Insufficient stock");

        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
                .orElseGet(() -> CartItem.builder().cart(cart).variant(variant).quantity(0).build());
        item.setQuantity(item.getQuantity() + req.quantity());
        cartItemRepository.save(item);
        return toResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(UUID userId, UUID itemId, UpdateCartItemRequest req) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart","user",userId));
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("CartItem","id",itemId));
        if (!item.getCart().getId().equals(cart.getId())) throw new ForbiddenException("Item not in your cart");
        if (req.quantity() <= 0) cartItemRepository.delete(item);
        else {
            if (item.getVariant().getStockQuantity() < req.quantity()) throw new BadRequestException("Insufficient stock");
            item.setQuantity(req.quantity());
        }
        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart","user",userId));
        CartItem item = cartItemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("CartItem","id",itemId));
        if (!item.getCart().getId().equals(cart.getId())) throw new ForbiddenException("Item not in your cart");
        cartItemRepository.delete(item);
        return toResponse(cart);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new ResourceNotFoundException("Cart","user",userId));
        cart.getItems().clear();
        cartItemRepository.flush();
    }

    public CartResponse toResponse(Cart cart) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(i -> i.getVariant().getEffectivePrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_FLAT;
        BigDecimal tax = subtotal.multiply(TAX_RATE);
        BigDecimal total = subtotal.add(shipping).add(tax);
        int count = cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            ProductVariant v = i.getVariant();
            Product p = v.getProduct();
            BigDecimal line = v.getEffectivePrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return new CartItemResponse(i.getId(), v.getId(), p.getId(), p.getName(),
                    v.getSize(), v.getColor(), v.getImageUrl() != null ? v.getImageUrl() : p.getMainImageUrl(),
                    v.getSku(), v.getEffectivePrice(), i.getQuantity(), line);
        }).toList();
        return new CartResponse(cart.getId(), items, subtotal, BigDecimal.ZERO, shipping, tax, total, count);
    }
}
