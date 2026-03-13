package com.loopy.review.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.entity.Card;
import com.loopy.card.repository.CardRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.review.dto.ReviewResponse;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final CardRepository cardRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final SM2Service sm2Service;

    public ReviewService(CardRepository cardRepository,
                         ReviewLogRepository reviewLogRepository,
                         SM2Service sm2Service) {
        this.cardRepository = cardRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.sm2Service = sm2Service;
    }

    /** Returns all cards due for review today (or overdue). */
    public List<CardResponse> getDueCards(User user) {
        return cardRepository.findDueCards(user.getId(), LocalDate.now()).stream()
                .map(CardResponse::from)
                .toList();
    }

    /** Submits a review: runs SM-2, updates card state, saves review log. */
    @Transactional
    public ReviewResponse submitReview(UUID cardId, SubmitReviewRequest request, User user) {
        Card card = cardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        LocalDate today = LocalDate.now();

        SM2Result result = sm2Service.calculate(
                card.getRepetitionCount(),
                card.getEaseFactor(),
                card.getIntervalDays(),
                request.rating(),
                today
        );

        // Update card with new SM-2 state
        card.setRepetitionCount(result.repetitionCount());
        card.setEaseFactor(result.easeFactor());
        card.setIntervalDays(result.intervalDays());
        card.setNextReviewDate(result.nextReviewDate());
        card.setLastReviewDate(today);
        cardRepository.save(card);

        // Save immutable review log
        ReviewLog log = new ReviewLog(card, user, request.rating(), request.responseTimeMs());
        reviewLogRepository.save(log);

        return ReviewResponse.from(log, card);
    }
}
