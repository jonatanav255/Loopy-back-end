package com.loopy.auth.repository;

// Dependencies: JpaRepository, derived query methods — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
