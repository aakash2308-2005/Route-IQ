package com.smartcommute.domain.ranking;

import java.util.Map;

/**
 * The score assigned to a route by a {@link RankingStrategy}.
 *
 * <p>{@code total} is the value used for ordering. {@code breakdown} exposes how each attribute
 * contributed, which makes ranking explainable (FR-30) — the API returns this so a user can see
 * <em>why</em> a route ranked where it did.</p>
 *
 * <p>Convention: <b>lower total is better</b> for cost/time-style strategies; the {@link RouteRanker}
 * orders ascending by total. Strategies that are naturally "higher is better" (reliability) invert
 * their value so the same ordering rule applies everywhere.</p>
 */
public record RouteScore(double total, Map<String, Double> breakdown) {

    public RouteScore {
        breakdown = breakdown == null ? Map.of() : Map.copyOf(breakdown);
    }
}
