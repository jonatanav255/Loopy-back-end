package com.loopy.teachback.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.Card;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Detects when a card crosses the escalation threshold and
 * sets its concept status to TEACH_BACK_REQUIRED.
 *
 * Threshold: 3+ failures (rating < 3) or 3+ low-confidence reviews (confidence = 1)
 * on any card within the concept.
 */
@Service
public class EscalationService {

    private static final int FAILURE_THRESHOLD = 3;

    private final ReviewLogRepository reviewLogRepository;
    private final ConceptRepository conceptRepository;

    public EscalationService(ReviewLogRepository reviewLogRepository,
                             ConceptRepository conceptRepository) {
        this.reviewLogRepository = reviewLogRepository;
        this.conceptRepository = conceptRepository;
    }

    /**
     * Checks if the card's concept should be escalated to TEACH_BACK_REQUIRED.
     * Called after each review submission.
     */
    @Transactional
    public void checkAndEscalate(Card card) {
        Concept concept = card.getConcept();

        // Don't re-escalate if already in teach-back
        if (concept.getStatus() == ConceptStatus.TEACH_BACK_REQUIRED) {
            return;
        }

        UUID userId = card.getUser().getId();
        List<ReviewLog> reviews = reviewLogRepository.findByCardIdAndUserIdOrderByReviewedAtDesc(
                card.getId(), userId);

        long failures = reviews.stream().filter(r -> r.getRating() < 3).count();
        long lowConfidence = reviews.stream()
                .filter(r -> r.getConfidence() != null && r.getConfidence() == 1)
                .count();

        if (failures >= FAILURE_THRESHOLD || lowConfidence >= FAILURE_THRESHOLD) {
            concept.setStatus(ConceptStatus.TEACH_BACK_REQUIRED);
            conceptRepository.save(concept);
        }
    }
}
