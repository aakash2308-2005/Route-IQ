package com.smartcommute.application.user;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.persistence.entity.PreferenceEntity;
import com.smartcommute.persistence.repository.PreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Manages a user's default preferences and merges them with per-request overrides into an effective
 * profile (docs/03-hld.md section 2.6; FR-17: overrides win over defaults).
 */
@Service
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public PreferenceService(PreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public PreferenceEntity getOrDefaults(long userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> new PreferenceEntity(userId));
    }

    @Transactional
    public PreferenceEntity update(long userId, RankingObjective objective, Integer maxWalkingMeters,
                                   Integer maxCostCents, Integer maxTransfers,
                                   Set<TransportMode> preferredModes, Set<TransportMode> avoidedModes) {
        PreferenceEntity entity = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> new PreferenceEntity(userId));
        entity.setObjective(objective != null ? objective : RankingObjective.BALANCED);
        entity.setMaxWalkingMeters(maxWalkingMeters);
        entity.setMaxCostCents(maxCostCents);
        entity.setMaxTransfers(maxTransfers);
        entity.setPreferredModes(preferredModes != null ? preferredModes : Set.of());
        entity.setAvoidedModes(avoidedModes != null ? avoidedModes : Set.of());
        return preferenceRepository.save(entity);
    }

    /** Build the soft-preference profile used by ranking. */
    public PreferenceProfile toProfile(PreferenceEntity entity, RankingObjective objectiveOverride) {
        RankingObjective objective = objectiveOverride != null ? objectiveOverride : entity.getObjective();
        return new PreferenceProfile(objective, entity.getPreferredModes(), entity.getAvoidedModes(),
                PreferenceProfile.DEFAULT_BALANCED_WEIGHTS);
    }
}
