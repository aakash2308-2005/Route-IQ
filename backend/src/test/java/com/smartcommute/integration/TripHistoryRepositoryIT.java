package com.smartcommute.integration;

import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.persistence.entity.TripHistoryEntity;
import com.smartcommute.persistence.entity.UserEntity;
import com.smartcommute.persistence.repository.TripHistoryRepository;
import com.smartcommute.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice against real Postgres (docs/09 T6.2). Verifies the paginated, ordered history
 * query that backs FR-36 and relies on idx_trip_history_user_created.
 */
class TripHistoryRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    TripHistoryRepository historyRepository;

    @Test
    void returnsHistoryNewestFirstPaginated() {
        UserEntity user = userRepository.save(
                new UserEntity("hist-" + System.nanoTime() + "@example.com", "hash", "Hist"));
        long userId = user.getId();

        for (int i = 0; i < 5; i++) {
            historyRepository.save(new TripHistoryEntity(userId, "Home", "Office",
                    RankingObjective.FASTEST, 600 + i, 1000, 200, 1));
        }

        Page<TripHistoryEntity> page = historyRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 3));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(3);
        // Newest first: created_at descending. The last-saved row should appear on the first page.
        assertThat(page.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(page.getContent().get(2).getCreatedAt());
    }
}
