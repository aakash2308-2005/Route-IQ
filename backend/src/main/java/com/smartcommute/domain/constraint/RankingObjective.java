package com.smartcommute.domain.constraint;

/**
 * The objective used to rank candidate routes (FR-16, FR-28). One ranking strategy exists per
 * objective (see docs/04-lld.md section 2.5 and docs/07-algorithms.md section 5).
 */
public enum RankingObjective {
    FASTEST,
    CHEAPEST,
    LEAST_WALKING,
    FEWEST_TRANSFERS,
    MOST_RELIABLE,
    BALANCED
}
