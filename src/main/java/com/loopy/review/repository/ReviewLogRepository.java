package com.loopy.review.repository;

// Dependencies: JpaRepository, @Query, @Param, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.review.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    List<ReviewLog> findByCardIdOrderByReviewedAtDesc(UUID cardId);

    /** Count reviews by a user within a time range. */
    long countByUserIdAndReviewedAtBetween(UUID userId, Instant start, Instant end);

    /** Count passed reviews (rating >= 3) by a user within a time range. */
    @Query("SELECT COUNT(r) FROM ReviewLog r WHERE r.user.id = :userId " +
           "AND r.reviewedAt BETWEEN :start AND :end AND r.rating >= 3")
    long countPassedByUserInRange(@Param("userId") UUID userId,
                                  @Param("start") Instant start,
                                  @Param("end") Instant end);

    /** All reviews by a user, ordered by time — used for streak and heatmap calculations. */
    @Query("SELECT r FROM ReviewLog r WHERE r.user.id = :userId ORDER BY r.reviewedAt ASC")
    List<ReviewLog> findAllByUserIdOrdered(@Param("userId") UUID userId);

    /**
     * Fragile knowledge: cards where the user got it right (rating >= 3) but had low confidence (1).
     * Returns the review logs for further processing.
     */
    @Query("SELECT r FROM ReviewLog r WHERE r.user.id = :userId " +
           "AND r.rating >= 3 AND r.confidence = 1 ORDER BY r.reviewedAt DESC")
    List<ReviewLog> findFragileReviews(@Param("userId") UUID userId);

    /** Reviews for a specific card by a specific user, most recent first. */
    List<ReviewLog> findByCardIdAndUserIdOrderByReviewedAtDesc(UUID cardId, UUID userId);
}
