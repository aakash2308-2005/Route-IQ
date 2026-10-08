package com.smartcommute.persistence.repository;

import com.smartcommute.persistence.entity.TripHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripHistoryRepository extends JpaRepository<TripHistoryEntity, Long> {

    /** Paginated recent trips (FR-36). Uses idx_trip_history_user_created (docs/05 section 7). */
    Page<TripHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
