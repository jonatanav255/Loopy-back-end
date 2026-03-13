package com.loopy.teachback.repository;

// Dependencies: JpaRepository, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.teachback.entity.TeachBack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeachBackRepository extends JpaRepository<TeachBack, UUID> {

    List<TeachBack> findByConceptIdAndUserIdOrderByCreatedAtDesc(UUID conceptId, UUID userId);

    List<TeachBack> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
