package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.TransportMode;

import java.util.List;

/**
 * A complete candidate journey: an ordered list of {@link Leg}s plus precomputed totals.
 *
 * <p>Totals are computed once at construction so that constraint checks (FR-21) and ranking
 * (FR-28) are cheap. Reliability of a path is the product of its legs' reliabilities (independent
 * legs assumption) — a simple, defensible model documented in docs/07-algorithms.md.</p>
 */
public record RoutePath(
        List<Leg> legs,
        int totalSeconds,
        int totalCostCents,
        int totalWalkingMeters,
        int transferCount,
        double reliability) {

    /** Builds a RoutePath from legs, computing all totals. */
    public static RoutePath of(List<Leg> legs) {
        int seconds = 0;
        int cost = 0;
        int walk = 0;
        int transfers = 0;
        double reliability = 1.0;
        for (Leg leg : legs) {
            seconds += leg.travelSeconds();
            cost += leg.costCents();
            if (leg.mode() == TransportMode.WALK) {
                walk += leg.distanceMeters();
            }
            if (leg.isTransfer()) {
                transfers++;
            }
        }
        return new RoutePath(List.copyOf(legs), seconds, cost, walk, transfers, reliability);
    }

    /**
     * Builds a RoutePath from legs and an explicit reliability value (computed by the engine from
     * edge reliabilities).
     */
    public static RoutePath of(List<Leg> legs, double reliability) {
        RoutePath base = of(legs);
        return new RoutePath(base.legs(), base.totalSeconds(), base.totalCostCents(),
                base.totalWalkingMeters(), base.transferCount(), reliability);
    }

    /**
     * Hard-constraint feasibility check (FR-21). Returns true only if the path violates none of the
     * caller's hard limits. Mode restrictions are enforced during search, so they are re-checked
     * here defensively.
     */
    public boolean satisfies(Constraints c) {
        if (c.maxWalkingMeters() != null && totalWalkingMeters > c.maxWalkingMeters()) {
            return false;
        }
        if (c.maxCostCents() != null && totalCostCents > c.maxCostCents()) {
            return false;
        }
        if (c.maxTransfers() != null && transferCount > c.maxTransfers()) {
            return false;
        }
        for (Leg leg : legs) {
            if (!c.allowsMode(leg.mode())) {
                return false;
            }
        }
        return true;
    }
}
