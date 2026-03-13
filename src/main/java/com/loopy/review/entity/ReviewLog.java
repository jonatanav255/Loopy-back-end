package com.loopy.review.entity;

// Dependencies: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Card;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of a single review attempt.
 * No updatedAt — once created, review logs are never modified.
 */
@Entity
@Table(name = "review_logs")
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, updatable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(nullable = false)
    private int rating;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column
    private Integer confidence;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt = Instant.now();

    public ReviewLog() {}

    public ReviewLog(Card card, User user, int rating, Long responseTimeMs, Integer confidence) {
        this.card = card;
        this.user = user;
        this.rating = rating;
        this.responseTimeMs = responseTimeMs;
        this.confidence = confidence;
    }

    public UUID getId() { return id; }
    public Card getCard() { return card; }
    public User getUser() { return user; }
    public int getRating() { return rating; }
    public Long getResponseTimeMs() { return responseTimeMs; }
    public Integer getConfidence() { return confidence; }
    public Instant getReviewedAt() { return reviewedAt; }
}
