package com.smartcommute.application.history;

import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.persistence.entity.TripHistoryEntity;
import com.smartcommute.persistence.repository.TripHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and lists a user's trip history (FR-35, FR-36).
 *
 * <p>Recording is {@link Async} and runs in its own transaction so a database hiccup while logging
 * a search never affects the user's route response (graceful degradation, docs/05 section 6).</p>
 */
@Service
public class TripHistoryService {

    private static final Logger log = LoggerFactory.getLogger(TripHistoryService.class);

    private final TripHistoryRepository repository;

    public TripHistoryService(TripHistoryRepository repository) {
        this.repository = repository;
    }

    @Async
    @Transactional
    public void recordAsync(long userId, String source, String dest, RankingObjective objective,
                            int totalSeconds, int totalCostCents, int totalWalkMeters, int transferCount) {
        try {
            repository.save(new TripHistoryEntity(userId, source, dest, objective,
                    totalSeconds, totalCostCents, totalWalkMeters, transferCount));
        } catch (Exception e) {
            // Logging failure must not surface to the user; the route result was already returned.
            log.warn("failed to record trip history for user {}", userId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<TripHistoryEntity> list(long userId, int page, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }
}
