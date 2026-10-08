package com.smartcommute.persistence.repository;

import com.smartcommute.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /** Login lookup. Backed by the unique index on email (docs/05 section 7). */
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
