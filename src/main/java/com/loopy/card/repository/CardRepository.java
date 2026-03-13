package com.loopy.card.repository;

// Dependencies: JpaRepository, @Query, @Param, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByConceptIdAndUserIdOrderByCreatedAtDesc(UUID conceptId, UUID userId);

    Optional<Card> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.nextReviewDate <= :today " +
           "ORDER BY c.nextReviewDate ASC, c.easeFactor ASC")
    List<Card> findDueCards(@Param("userId") UUID userId, @Param("today") LocalDate today);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.nextReviewDate <= :today " +
           "AND c.concept.topic.id IN :topicIds " +
           "ORDER BY c.nextReviewDate ASC, c.easeFactor ASC")
    List<Card> findDueCardsByTopics(@Param("userId") UUID userId, @Param("today") LocalDate today, @Param("topicIds") List<UUID> topicIds);

    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.concept.topic.id = :topicId")
    long countByTopicId(@Param("topicId") UUID topicId);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId " +
           "AND c.concept.topic.id IN :topicIds " +
           "ORDER BY c.createdAt DESC")
    List<Card> findCardsByTopics(@Param("userId") UUID userId, @Param("topicIds") List<UUID> topicIds);

    @Query("SELECT c FROM Card c JOIN FETCH c.concept con JOIN FETCH con.topic " +
           "WHERE c.user.id = :userId " +
           "AND (LOWER(c.front) LIKE LOWER(:pattern) OR LOWER(c.back) LIKE LOWER(:pattern) " +
           "OR LOWER(c.hint) LIKE LOWER(:pattern)) " +
           "ORDER BY c.createdAt DESC")
    List<Card> searchByUser(@Param("userId") UUID userId, @Param("pattern") String pattern);
}
