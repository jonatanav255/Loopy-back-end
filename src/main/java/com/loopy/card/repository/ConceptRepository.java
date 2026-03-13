package com.loopy.card.repository;

// Dependencies: JpaRepository, @Query, @EntityGraph, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptRepository extends JpaRepository<Concept, UUID> {

    List<Concept> findByTopicIdAndUserIdOrderByTitleAsc(UUID topicId, UUID userId);

    Optional<Concept> findByIdAndUserId(UUID id, UUID userId);

    /** Fetches concept with its topic eagerly loaded (avoids LazyInitializationException). */
    @EntityGraph(attributePaths = "topic")
    Optional<Concept> findWithTopicByIdAndUserId(UUID id, UUID userId);

    List<Concept> findByUserIdAndStatusOrderByUpdatedAtDesc(UUID userId, ConceptStatus status);
}
