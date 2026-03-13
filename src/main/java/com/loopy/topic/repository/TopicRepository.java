package com.loopy.topic.repository;

// Dependencies: JpaRepository, @Query, @Param, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByUserIdOrderBySortOrderAsc(UUID userId);

    Optional<Topic> findByIdAndUserId(UUID id, UUID userId);

    List<Topic> findAllByUserIdAndIdIn(UUID userId, List<UUID> ids);

    @Query("SELECT t FROM Topic t WHERE t.user.id = :userId " +
           "AND (LOWER(t.name) LIKE LOWER(:pattern) OR LOWER(t.description) LIKE LOWER(:pattern)) " +
           "ORDER BY t.name ASC")
    List<Topic> searchByUser(@Param("userId") UUID userId, @Param("pattern") String pattern);
}
