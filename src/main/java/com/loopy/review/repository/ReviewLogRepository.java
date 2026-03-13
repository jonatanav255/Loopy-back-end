package com.loopy.review.repository;

// Dependencies: JpaRepository, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.review.entity.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, UUID> {

    List<ReviewLog> findByCardIdOrderByReviewedAtDesc(UUID cardId);
}
