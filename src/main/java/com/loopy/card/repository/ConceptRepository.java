package com.loopy.card.repository;

// Dependencies: JpaRepository, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.card.entity.Concept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptRepository extends JpaRepository<Concept, UUID> {

    List<Concept> findByTopicIdAndUserIdOrderByTitleAsc(UUID topicId, UUID userId);

    Optional<Concept> findByIdAndUserId(UUID id, UUID userId);
}
