package com.smartcommute.application.route;

import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;

import java.time.LocalTime;
import java.util.Set;

/**
 * A validated route-planning request at the application boundary (docs/04-lld.md section 3.1).
 * Built by the web layer from the incoming DTO; the domain never sees HTTP types.
 */
public record PlanRouteCommand(
        long userId,
        String source,
        String destination,
        LocalTime departAt,
        LocalTime arrivalBefore,
        Set<TransportMode> allowedModes,
        Integer maxWalkingMeters,
        Integer maxCostCents,
        Integer maxTransfers,
        RankingObjective objective,
        int k) {

    /** The wall-clock time used to look up traffic/ETA: departure if given, else arrival, else now. */
    public LocalTime effectiveTime() {
        if (departAt != null) {
            return departAt;
        }
        if (arrivalBefore != null) {
            return arrivalBefore;
        }
        return LocalTime.now();
    }
}
