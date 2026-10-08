package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.RankingObjective;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that maps a {@link RankingObjective} to its {@link RankingStrategy} (docs/04-lld.md
 * section 2.5). Uses an {@link EnumMap} for O(1) lookup and centralizes wiring so callers never
 * switch on the objective themselves.
 */
public final class RankingStrategyFactory {

    private final Map<RankingObjective, RankingStrategy> strategies = new EnumMap<>(RankingObjective.class);

    public RankingStrategyFactory() {
        register(new SingleAttributeStrategies.FastestStrategy());
        register(new SingleAttributeStrategies.CheapestStrategy());
        register(new SingleAttributeStrategies.LeastWalkingStrategy());
        register(new SingleAttributeStrategies.FewestTransfersStrategy());
        register(new SingleAttributeStrategies.MostReliableStrategy());
        register(new BalancedStrategy());
    }

    private void register(RankingStrategy strategy) {
        strategies.put(strategy.objective(), strategy);
    }

    public RankingStrategy forObjective(RankingObjective objective) {
        RankingStrategy strategy = strategies.get(objective);
        if (strategy == null) {
            throw new IllegalArgumentException("no ranking strategy for objective: " + objective);
        }
        return strategy;
    }
}
