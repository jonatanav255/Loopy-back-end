package com.loopy.review.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @Valid, @RequestBody, @RequestParam, @PathVariable, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.review.dto.ReviewResponse;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Returns cards due for review today, optionally filtered by topics and limited. */
    @GetMapping("/today")
    public ResponseEntity<List<CardResponse>> today(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) List<UUID> topicIds,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.getDueCards(user, topicIds, limit));
    }

    /** Submits a review for a card, applying SM-2 scheduling. */
    @PostMapping("/{cardId}")
    public ResponseEntity<ReviewResponse> submit(@PathVariable UUID cardId,
                                                 @Valid @RequestBody SubmitReviewRequest request,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.submitReview(cardId, request, user));
    }
}
