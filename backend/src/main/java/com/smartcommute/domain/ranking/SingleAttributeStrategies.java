package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.routing.RoutePath;

import java.util.List;
import java.util.Map;

/**
 * The five single-attribute ranking strategies. Each orders by exactly one path attribute and is a
 * trivial O(1) score, so we keep them together to avoid five near-identical files.
 *
 * <p>Reliability is "higher is better", so {@link MostReliableStrategy} inverts it (1 - reliability)
 * to fit the shared "lower total ranks first" convention.</p>
 */
public final class SingleAttributeStrategies {

    private SingleAttributeStrategies() {
    }

    public static final class FastestStrategy implements RankingStrategy {
        @Override
        public RankingObjective objective() {
            return RankingObjective.FASTEST;
        }

        @Override
        public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
            double total = path.totalSeconds();
            return new RouteScore(total, Map.of("time", total));
        }
    }

    public static final class CheapestStrategy implements RankingStrategy {
        @Override
        public RankingObjective objective() {
            return RankingObjective.CHEAPEST;
        }

        @Override
        public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
            double total = path.totalCostCents();
            return new RouteScore(total, Map.of("cost", total));
        }
    }

    public static final class LeastWalkingStrategy implements RankingStrategy {
        @Override
        public RankingObjective objective() {
            return RankingObjective.LEAST_WALKING;
        }

        @Override
        public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
            double total = path.totalWalkingMeters();
            return new RouteScore(total, Map.of("walk", total));
        }
    }

    public static final class FewestTransfersStrategy implements RankingStrategy {
        @Override
        public RankingObjective objective() {
            return RankingObjective.FEWEST_TRANSFERS;
        }

        @Override
        public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
            double total = path.transferCount();
            return new RouteScore(total, Map.of("transfers", total));
        }
    }

    public static final class MostReliableStrategy implements RankingStrategy {
        @Override
        public RankingObjective objective() {
            return RankingObjective.MOST_RELIABLE;
        }

        @Override
        public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
            // Higher reliability is better -> invert so lower total ranks first.
            double total = 1.0 - path.reliability();
            return new RouteScore(total, Map.of("reliability", path.reliability()));
        }
    }
}
