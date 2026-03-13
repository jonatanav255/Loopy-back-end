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
import com.loopy.teachback.service.EscalationService;
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
    private final FSRSService fsrsService;
    private final EscalationService escalationService;

    public ReviewService(CardRepository cardRepository,
                         ReviewLogRepository reviewLogRepository,
                         SM2Service sm2Service,
                         FSRSService fsrsService,
                         EscalationService escalationService) {
        this.cardRepository = cardRepository;
        this.reviewLogRepository = reviewLogRepository;
        this.sm2Service = sm2Service;
        this.fsrsService = fsrsService;
        this.escalationService = escalationService;
    }

    /** Returns cards due for review today (or overdue), optionally filtered by topics and limited. */
    public List<CardResponse> getDueCards(User user, List<UUID> topicIds, Integer limit) {
        List<Card> cards;
        if (topicIds != null && !topicIds.isEmpty()) {
            cards = cardRepository.findDueCardsByTopics(user.getId(), LocalDate.now(), topicIds);
        } else {
            cards = cardRepository.findDueCards(user.getId(), LocalDate.now());
        }

        var stream = cards.stream().map(CardResponse::from);
        if (limit != null && limit > 0) {
            stream = stream.limit(limit);
        }
        return stream.toList();
    }

    /** Submits a review: runs the card's scheduling algorithm, updates state, saves log. */
    @Transactional
    public ReviewResponse submitReview(UUID cardId, SubmitReviewRequest request, User user) {
        Card card = cardRepository.findByIdAndUserId(cardId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        LocalDate today = LocalDate.now();

        SchedulingResult result = schedule(card, request.rating(), today);

        // Update card with new state
        card.setRepetitionCount(result.repetitionCount());
        card.setEaseFactor(result.easeFactor());
        card.setIntervalDays(result.intervalDays());
        card.setNextReviewDate(result.nextReviewDate());
        card.setLastReviewDate(today);
        card.setStability(result.stability());
        card.setDifficulty(result.difficulty());
        cardRepository.save(card);

        // Save immutable review log
        ReviewLog log = new ReviewLog(card, user, request.rating(), request.responseTimeMs(), request.confidence());
        reviewLogRepository.save(log);

        // Check if concept should be escalated to teach-back
        escalationService.checkAndEscalate(card);

        return ReviewResponse.from(log, card);
    }

    /** Routes to the correct algorithm based on the card's scheduling setting. */
    private SchedulingResult schedule(Card card, int rating, LocalDate today) {
        if (card.getSchedulingAlgorithm() == SchedulingAlgorithm.FSRS) {
            return fsrsService.calculate(
                    card.getStability(),
                    card.getDifficulty(),
                    card.getIntervalDays(),
                    rating,
                    card.getRepetitionCount(),
                    today
            );
        }

        // Default: SM-2
        SM2Result sm2 = sm2Service.calculate(
                card.getRepetitionCount(),
                card.getEaseFactor(),
                card.getIntervalDays(),
                rating,
                today
        );
        return new SchedulingResult(
                sm2.repetitionCount(),
                sm2.easeFactor(),
                sm2.intervalDays(),
                sm2.nextReviewDate(),
                card.getStability(),
                card.getDifficulty()
        );
    }
}
