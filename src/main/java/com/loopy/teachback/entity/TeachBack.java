package com.loopy.teachback.entity;

// Dependencies: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Concept;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a teach-back session: user explains a concept, self-rates,
 * and marks gaps found. Immutable — no updatedAt.
 */
@Entity
@Table(name = "teach_backs")
public class TeachBack {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_id", nullable = false, updatable = false)
    private Concept concept;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "user_explanation", nullable = false, columnDefinition = "TEXT")
    private String userExplanation;

    @Column(name = "self_rating", nullable = false)
    private int selfRating;

    /** Comma-separated list of gap descriptions. */
    @Column(name = "gaps_found", columnDefinition = "TEXT")
    private String gapsFound;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TeachBack() {}

    public TeachBack(Concept concept, User user, String userExplanation, int selfRating, String gapsFound) {
        this.concept = concept;
        this.user = user;
        this.userExplanation = userExplanation;
        this.selfRating = selfRating;
        this.gapsFound = gapsFound;
    }

    public UUID getId() { return id; }
    public Concept getConcept() { return concept; }
    public User getUser() { return user; }
    public String getUserExplanation() { return userExplanation; }
    public int getSelfRating() { return selfRating; }
    public String getGapsFound() { return gapsFound; }
    public Instant getCreatedAt() { return createdAt; }
}
