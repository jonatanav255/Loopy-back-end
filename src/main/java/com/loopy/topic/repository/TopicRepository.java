package com.loopy.topic.repository;

// Dependencies: JpaRepository, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByUserIdOrderByNameAsc(UUID userId);

    Optional<Topic> findByIdAndUserId(UUID id, UUID userId);
}
