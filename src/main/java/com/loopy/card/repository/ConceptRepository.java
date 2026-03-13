package com.loopy.card.repository;

// Dependencies: JpaRepository, @Query, @Param, @EntityGraph, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptRepository extends JpaRepository<Concept, UUID> {

    List<Concept> findByTopicIdAndUserIdOrderBySortOrderAsc(UUID topicId, UUID userId);

    Optional<Concept> findByIdAndUserId(UUID id, UUID userId);

    List<Concept> findAllByUserIdAndIdIn(UUID userId, List<UUID> ids);

    /** Fetches concept with its topic eagerly loaded (avoids LazyInitializationException). */
    @EntityGraph(attributePaths = "topic")
    Optional<Concept> findWithTopicByIdAndUserId(UUID id, UUID userId);

    List<Concept> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, ConceptStatus status);

    @Query("SELECT c FROM Concept c JOIN FETCH c.topic WHERE c.user.id = :userId " +
           "AND (LOWER(c.title) LIKE LOWER(:pattern) OR LOWER(c.notes) LIKE LOWER(:pattern)) " +
           "ORDER BY c.title ASC")
    List<Concept> searchByUser(@Param("userId") UUID userId, @Param("pattern") String pattern);
}
