package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.routing.RoutePath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks candidate paths for a given objective (docs/07-algorithms.md section 5).
 *
 * <p>Uses the {@link RankingStrategyFactory} to pick the strategy, scores every path against the
 * cohort, then sorts ascending by score total. Ties are broken deterministically (by total time,
 * then cost, then transfer count) so the output is reproducible — important for caching and for
 * predictable tests.</p>
 */
public final class RouteRanker {

    private final RankingStrategyFactory strategyFactory;

    public RouteRanker(RankingStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public RouteRanker() {
        this(new RankingStrategyFactory());
    }

    public List<RankedRoute> rank(List<RoutePath> paths, PreferenceProfile prefs) {
        if (paths.isEmpty()) {
            return List.of();
        }
        RankingStrategy strategy = strategyFactory.forObjective(prefs.objective());

        List<Scored> scored = new ArrayList<>(paths.size());
        for (RoutePath path : paths) {
            scored.add(new Scored(path, strategy.score(path, prefs, paths)));
        }

        scored.sort(Comparator
                .comparingDouble((Scored s) -> s.score.total())
                .thenComparingInt(s -> s.path.totalSeconds())
                .thenComparingInt(s -> s.path.totalCostCents())
                .thenComparingInt(s -> s.path.transferCount()));

        List<RankedRoute> ranked = new ArrayList<>(scored.size());
        int rank = 1;
        for (Scored s : scored) {
            ranked.add(new RankedRoute(rank++, s.path, s.score));
        }
        return ranked;
    }

    private record Scored(RoutePath path, RouteScore score) {
    }
}
