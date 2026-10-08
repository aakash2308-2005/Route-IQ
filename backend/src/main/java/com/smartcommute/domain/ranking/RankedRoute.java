package com.smartcommute.domain.ranking;

import com.smartcommute.domain.routing.RoutePath;

/**
 * A route paired with the score that determined its rank. {@code rank} is 1-based (1 = best).
 */
public record RankedRoute(int rank, RoutePath path, RouteScore score) {
}
