package com.smartcommute.domain.constraint;

import com.smartcommute.domain.graph.TransportMode;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * HARD constraints. A route that violates any of these is invalid and must never be returned
 * (FR-21). Contrast with {@link PreferenceProfile}, which holds SOFT preferences that only affect
 * ranking.
 *
 * <p>Nullable numeric fields mean "no limit". An empty {@code allowedModes} means "all modes
 * allowed".</p>
 *
 * @param maxWalkingMeters max total walking distance, or null for no limit
 * @param maxCostCents     max total cost in cents, or null for no limit
 * @param maxTransfers     max number of transfers, or null for no limit
 * @param allowedModes     modes that may be used; empty = all allowed
 */
public record Constraints(
        Integer maxWalkingMeters,
        Integer maxCostCents,
        Integer maxTransfers,
        Set<TransportMode> allowedModes) {

    public Constraints {
        if (maxWalkingMeters != null && maxWalkingMeters < 0) {
            throw new IllegalArgumentException("maxWalkingMeters must be >= 0");
        }
        if (maxCostCents != null && maxCostCents < 0) {
            throw new IllegalArgumentException("maxCostCents must be >= 0");
        }
        if (maxTransfers != null && maxTransfers < 0) {
            throw new IllegalArgumentException("maxTransfers must be >= 0");
        }
        if (allowedModes == null || allowedModes.isEmpty()) {
            allowedModes = Collections.emptySet();
        } else {
            allowedModes = Collections.unmodifiableSet(EnumSet.copyOf(allowedModes));
        }
    }

    /** No constraints at all. */
    public static Constraints none() {
        return new Constraints(null, null, null, Collections.emptySet());
    }

    /** True if the given mode is permitted (empty allow-set means all are permitted). */
    public boolean allowsMode(TransportMode mode) {
        return allowedModes.isEmpty() || allowedModes.contains(mode);
    }
}
