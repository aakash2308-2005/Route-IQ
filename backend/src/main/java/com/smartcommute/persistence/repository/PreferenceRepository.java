package com.smartcommute.persistence.repository;

import com.smartcommute.persistence.entity.PreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferenceRepository extends JpaRepository<PreferenceEntity, Long> {

    Optional<PreferenceEntity> findByUserId(Long userId);
}
