package com.smartcommute.persistence.repository;

import com.smartcommute.persistence.entity.SavedPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlaceEntity, Long> {

    List<SavedPlaceEntity> findByUserId(Long userId);

    Optional<SavedPlaceEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndLabel(Long userId, String label);
}
