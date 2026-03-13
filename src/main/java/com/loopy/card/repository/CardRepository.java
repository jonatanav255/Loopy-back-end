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

    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
