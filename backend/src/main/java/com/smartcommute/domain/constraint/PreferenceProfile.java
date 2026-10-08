package com.smartcommute.domain.constraint;

import com.smartcommute.domain.graph.TransportMode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * SOFT preferences. These influence ranking only; they never make a route invalid (FR-15).
 *
 * @param objective       the ranking objective to apply
 * @param preferredModes  modes the user prefers (small ranking bonus)
 * @param avoidedModes    modes the user prefers to avoid (small ranking penalty)
 * @param balancedWeights weights for the BALANCED objective, keyed by attribute name
 *                        ("time","cost","walk","transfers","reliability")
 */
public record PreferenceProfile(
        RankingObjective objective,
        Set<TransportMode> preferredModes,
        Set<TransportMode> avoidedModes,
        Map<String, Double> balancedWeights) {

    /** Documented default weights for BALANCED scoring (see docs/07-algorithms.md section 5.2). */
    public static final Map<String, Double> DEFAULT_BALANCED_WEIGHTS = Map.of(
            "time", 0.35,
            "cost", 0.25,
            "walk", 0.15,
            "transfers", 0.10,
            "reliability", 0.15);

    public PreferenceProfile {
        if (objective == null) {
            objective = RankingObjective.BALANCED;
        }
        preferredModes = freeze(preferredModes);
        avoidedModes = freeze(avoidedModes);
        balancedWeights = (balancedWeights == null || balancedWeights.isEmpty())
                ? DEFAULT_BALANCED_WEIGHTS
                : Collections.unmodifiableMap(Map.copyOf(balancedWeights));
    }

    private static Set<TransportMode> freeze(Set<TransportMode> modes) {
        if (modes == null || modes.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(modes));
    }

    public static PreferenceProfile defaults() {
        return new PreferenceProfile(RankingObjective.BALANCED,
                Collections.emptySet(), Collections.emptySet(), DEFAULT_BALANCED_WEIGHTS);
    }

    public static PreferenceProfile forObjective(RankingObjective objective) {
        return new PreferenceProfile(objective,
                Collections.emptySet(), Collections.emptySet(), DEFAULT_BALANCED_WEIGHTS);
    }
}
