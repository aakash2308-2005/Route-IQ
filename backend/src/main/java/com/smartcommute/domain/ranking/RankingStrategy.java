package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.routing.RoutePath;

import java.util.List;

/**
 * Strategy pattern for ranking (docs/04-lld.md section 2.5). Each objective has one strategy.
 * Adding a new objective (e.g. "least CO2") means adding a new strategy — no change to the ranker
 * (Open/Closed principle).
 *
 * <p>Ordering convention: <b>lower {@link RouteScore#total()} ranks first</b>.</p>
 */
public interface RankingStrategy {

    /** The objective this strategy implements. */
    RankingObjective objective();

    /**
     * Score a single path relative to the whole candidate cohort (needed for normalization in the
     * BALANCED strategy) and the user's soft preferences.
     *
     * @param path   the path to score
     * @param prefs  soft preferences (preferred/avoided modes, weights)
     * @param cohort all candidate paths being ranked together
     */
    RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort);
}
