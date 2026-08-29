package com.clothing.ai.review.service;

import com.clothing.ai.catalog.entity.Product;
import com.clothing.ai.catalog.repository.ProductRepository;
import com.clothing.ai.common.exception.*;
import com.clothing.ai.common.response.PageResponse;
import com.clothing.ai.review.dto.ReviewDtos.*;
import com.clothing.ai.review.entity.Review;
import com.clothing.ai.review.repository.ReviewRepository;
import com.clothing.ai.user.entity.User;
import com.clothing.ai.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse create(UUID userId, ReviewRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User","id",userId));
        Product product = productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product","id",req.productId()));

        // One review per user per product — update if exists, throw conflict on second attempt from different context
        boolean alreadyExists = reviewRepository.findByUserIdAndProductId(userId, req.productId()).isPresent();
        Review review = reviewRepository.findByUserIdAndProductId(userId, req.productId())
                .orElseGet(() -> Review.builder().user(user).product(product).build());

        review.setRating(req.rating());
        review.setTitle(req.title());
        review.setComment(req.comment());
        review.setSizeFit(req.sizeFit());
        review.setQuality(req.quality());
        review.setVerifiedPurchase(req.orderItemId() != null);
        review.setApproved(true);
        review = reviewRepository.save(review);
        recomputeProductRating(product);
        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listForProduct(UUID productId, int page, int size) {
        return PageResponse.from(reviewRepository.findByProductIdAndApprovedTrue(productId, PageRequest.of(page, size)), this::toResponse);
    }

    @Transactional
    public void helpful(UUID id) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review","id",id));
        r.setHelpfulCount(r.getHelpfulCount() + 1);
    }

    @Transactional
    public void delete(UUID id) {
        Review r = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review","id",id));
        Product product = r.getProduct();
        reviewRepository.delete(r);
        recomputeProductRating(product);
    }

    private void recomputeProductRating(Product p) {
        long count = reviewRepository.countByProductId(p.getId());
        if (count == 0) { p.setAverageRating(BigDecimal.ZERO); p.setReviewCount(0); return; }
        var avg = reviewRepository.findByProductId(p.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                .getContent().stream().mapToInt(Review::getRating).average().orElse(0);
        p.setAverageRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        p.setReviewCount((int) count);
    }

    private ReviewResponse toResponse(Review r) {
        String name = r.getUser().getFirstName() != null ? r.getUser().getFirstName() : "Anonymous";
        return new ReviewResponse(r.getId(), r.getProduct().getId(), r.getProduct().getName(),
                r.getUser().getId(), name, r.getUser().getAvatarUrl(),
                r.getRating(), r.getTitle(), r.getComment(), r.getSizeFit(), r.getQuality(),
                r.isVerifiedPurchase(), r.getHelpfulCount(), r.getCreatedAt());
    }
}
