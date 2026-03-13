package com.loopy.card.entity;

// Dependencies: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn, @Enumerated, @EntityListeners, @CreatedDate, @LastModifiedDate — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.topic.entity.Topic;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "concepts")
@EntityListeners(AuditingEntityListener.class)
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConceptStatus status = ConceptStatus.LEARNING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Concept() {}

    public Concept(Topic topic, User user, String title, String notes) {
        this.topic = topic;
        this.user = user;
        this.title = title;
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public Topic getTopic() { return topic; }
    public User getUser() { return user; }
    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public ConceptStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setTitle(String title) { this.title = title; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setStatus(ConceptStatus status) { this.status = status; }
}
