package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.domain.routing.Leg;
import com.smartcommute.domain.routing.RoutePath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BALANCED ranking: a transparent, explainable weighted sum of normalized attributes
 * (docs/07-algorithms.md section 5.2). This is deliberately NOT machine learning — ranking must be
 * explainable and testable (FR-30).
 *
 * <p>Algorithm:</p>
 * <ol>
 *   <li>Min-max normalize each attribute (time, cost, walk, transfers) across the candidate cohort
 *       to [0,1], so units with large magnitudes (seconds) do not dominate small ones
 *       (transfers).</li>
 *   <li>Reliability is already in [0,1]; we use {@code 1 - reliability} so all attributes are
 *       "lower is better".</li>
 *   <li>Combine with the user's weights (defaults documented in {@link PreferenceProfile}).</li>
 *   <li>Apply small soft-preference adjustments for preferred/avoided modes (never invalidating).</li>
 * </ol>
 *
 * <p>Normalization is cohort-relative by design: we are ranking these alternatives against each
 * other, not against an absolute scale.</p>
 */
public final class BalancedStrategy implements RankingStrategy {

    private static final double PREFERENCE_BONUS = 0.05;
    private static final double AVOIDANCE_PENALTY = 0.05;

    @Override
    public RankingObjective objective() {
        return RankingObjective.BALANCED;
    }

    @Override
    public RouteScore score(RoutePath path, PreferenceProfile prefs, List<RoutePath> cohort) {
        double timeN = normalize(path.totalSeconds(), cohort, RoutePath::totalSeconds);
        double costN = normalize(path.totalCostCents(), cohort, RoutePath::totalCostCents);
        double walkN = normalize(path.totalWalkingMeters(), cohort, RoutePath::totalWalkingMeters);
        double transferN = normalize(path.transferCount(), cohort, RoutePath::transferCount);
        double reliabilityN = 1.0 - path.reliability(); // lower is better

        Map<String, Double> w = prefs.balancedWeights();
        double wTime = w.getOrDefault("time", 0.0);
        double wCost = w.getOrDefault("cost", 0.0);
        double wWalk = w.getOrDefault("walk", 0.0);
        double wTransfers = w.getOrDefault("transfers", 0.0);
        double wReliability = w.getOrDefault("reliability", 0.0);

        double timeContribution = wTime * timeN;
        double costContribution = wCost * costN;
        double walkContribution = wWalk * walkN;
        double transferContribution = wTransfers * transferN;
        double reliabilityContribution = wReliability * reliabilityN;

        double softAdjustment = softPreferenceAdjustment(path, prefs);

        double total = timeContribution + costContribution + walkContribution
                + transferContribution + reliabilityContribution + softAdjustment;

        Map<String, Double> breakdown = new HashMap<>();
        breakdown.put("time", round(timeContribution));
        breakdown.put("cost", round(costContribution));
        breakdown.put("walk", round(walkContribution));
        breakdown.put("transfers", round(transferContribution));
        breakdown.put("reliability", round(reliabilityContribution));
        breakdown.put("preferenceAdjustment", round(softAdjustment));
        return new RouteScore(round(total), breakdown);
    }

    /** Adds a bonus (negative, since lower is better) for preferred modes, penalty for avoided ones. */
    private double softPreferenceAdjustment(RoutePath path, PreferenceProfile prefs) {
        double adjustment = 0.0;
        for (Leg leg : path.legs()) {
            TransportMode mode = leg.mode();
            if (prefs.preferredModes().contains(mode)) {
                adjustment -= PREFERENCE_BONUS;
            }
            if (prefs.avoidedModes().contains(mode)) {
                adjustment += AVOIDANCE_PENALTY;
            }
        }
        return adjustment;
    }

    private interface AttributeExtractor {
        double value(RoutePath path);
    }

    /** Min-max normalization; returns 0 when all cohort values are equal (no discriminating power). */
    private static double normalize(double value, List<RoutePath> cohort, AttributeExtractor extractor) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (RoutePath p : cohort) {
            double v = extractor.value(p);
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (max - min < 1e-9) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
