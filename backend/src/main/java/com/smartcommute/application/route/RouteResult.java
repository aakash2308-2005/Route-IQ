package com.smartcommute.application.route;

import com.smartcommute.domain.graph.TransportMode;

import java.util.List;
import java.util.Map;

/**
 * A ranked route returned to the caller (docs/04-lld.md section 3.1). Node ids are resolved to human
 * stop names in the {@code from}/{@code to} fields so the API is readable.
 */
public record RouteResult(
        int rank,
        int totalSeconds,
        int totalCostCents,
        int totalWalkingMeters,
        int transferCount,
        double reliability,
        double scoreTotal,
        Map<String, Double> scoreBreakdown,
        List<LegView> legs) {

    public record LegView(
            TransportMode mode,
            String from,
            String to,
            int travelSeconds,
            int costCents,
            int distanceMeters,
            boolean isTransfer) {
    }
}
