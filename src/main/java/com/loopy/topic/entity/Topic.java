package com.loopy.topic.entity;

// Dependencies: @Entity, @Table, @Id, @GeneratedValue, @Column, @ManyToOne, @JoinColumn, @EntityListeners, @CreatedDate, @LastModifiedDate — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topics")
@EntityListeners(AuditingEntityListener.class)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "color_hex", length = 7)
    private String colorHex = "#6366F1";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Topic() {}

    public Topic(User user, String name, String description, String colorHex) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.colorHex = colorHex;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getColorHex() { return colorHex; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
}
