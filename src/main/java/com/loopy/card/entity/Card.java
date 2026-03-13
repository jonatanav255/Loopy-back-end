package com.loopy.card.entity;

// Dependencies: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn, @Enumerated, @EntityListeners, @CreatedDate, @LastModifiedDate — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
@EntityListeners(AuditingEntityListener.class)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false)
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String front;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String back;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 30)
    private CardType cardType = CardType.STANDARD;

    @Column(columnDefinition = "TEXT")
    private String hint;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "repetition_count", nullable = false)
    private int repetitionCount = 0;

    @Column(name = "ease_factor", nullable = false)
    private double easeFactor = 2.5;

    @Column(name = "interval_days", nullable = false)
    private int intervalDays = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Card() {}

    public Card(Concept concept, User user, String front, String back, CardType cardType,
                String hint, String sourceUrl) {
        this.concept = concept;
        this.user = user;
        this.front = front;
        this.back = back;
        this.cardType = cardType;
        this.hint = hint;
        this.sourceUrl = sourceUrl;
    }

    public UUID getId() { return id; }
    public Concept getConcept() { return concept; }
    public User getUser() { return user; }
    public String getFront() { return front; }
    public String getBack() { return back; }
    public CardType getCardType() { return cardType; }
    public String getHint() { return hint; }
    public String getSourceUrl() { return sourceUrl; }
    public int getRepetitionCount() { return repetitionCount; }
    public double getEaseFactor() { return easeFactor; }
    public int getIntervalDays() { return intervalDays; }
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public LocalDate getLastReviewDate() { return lastReviewDate; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setFront(String front) { this.front = front; }
    public void setBack(String back) { this.back = back; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }
    public void setHint(String hint) { this.hint = hint; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setRepetitionCount(int repetitionCount) { this.repetitionCount = repetitionCount; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }
    public void setIntervalDays(int intervalDays) { this.intervalDays = intervalDays; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }
    public void setLastReviewDate(LocalDate lastReviewDate) { this.lastReviewDate = lastReviewDate; }
}
