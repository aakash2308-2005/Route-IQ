package com.smartcommute.domain.ranking;

import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.domain.routing.Leg;
import com.smartcommute.domain.routing.RoutePath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RouteRankerTest {

    private RoutePath path(int seconds, int cost, int walk, int transfers, double reliability) {
        // A single synthetic leg carrying the aggregate attributes we want to test ranking against.
        Leg leg = new Leg(TransportMode.METRO, 1, 2, seconds, cost, 0, false);
        List<Leg> legs = List.of(leg);
        return new RoutePath(legs, seconds, cost, walk, transfers, reliability);
    }

    private final RouteRanker ranker = new RouteRanker();

    @Test
    void fastestRanksByTime() {
        RoutePath slow = path(1200, 100, 0, 0, 0.9);
        RoutePath fast = path(600, 500, 0, 0, 0.9);
        List<RankedRoute> ranked = ranker.rank(List.of(slow, fast),
                PreferenceProfile.forObjective(RankingObjective.FASTEST));
        assertThat(ranked.get(0).path()).isEqualTo(fast);
        assertThat(ranked.get(0).rank()).isEqualTo(1);
    }

    @Test
    void cheapestRanksByCost() {
        RoutePath cheap = path(1200, 100, 0, 0, 0.9);
        RoutePath pricey = path(600, 500, 0, 0, 0.9);
        List<RankedRoute> ranked = ranker.rank(List.of(pricey, cheap),
                PreferenceProfile.forObjective(RankingObjective.CHEAPEST));
        assertThat(ranked.get(0).path()).isEqualTo(cheap);
    }

    @Test
    void mostReliableRanksByReliabilityDescending() {
        RoutePath reliable = path(1000, 100, 0, 0, 0.95);
        RoutePath flaky = path(600, 100, 0, 0, 0.60);
        List<RankedRoute> ranked = ranker.rank(List.of(flaky, reliable),
                PreferenceProfile.forObjective(RankingObjective.MOST_RELIABLE));
        assertThat(ranked.get(0).path()).isEqualTo(reliable);
    }

    @Test
    void balancedBreakdownIsPopulatedAndDeterministic() {
        RoutePath a = path(600, 300, 200, 1, 0.9);
        RoutePath b = path(900, 200, 400, 0, 0.8);
        List<RankedRoute> first = ranker.rank(List.of(a, b),
                PreferenceProfile.forObjective(RankingObjective.BALANCED));
        List<RankedRoute> second = ranker.rank(List.of(a, b),
                PreferenceProfile.forObjective(RankingObjective.BALANCED));

        assertThat(first.get(0).score().breakdown()).containsKeys("time", "cost", "walk", "transfers", "reliability");
        // Deterministic: same input -> same ordering.
        assertThat(first.get(0).path()).isEqualTo(second.get(0).path());
    }

    @Test
    void emptyInputYieldsEmptyRanking() {
        assertThat(ranker.rank(List.of(), PreferenceProfile.defaults())).isEmpty();
    }

    @Test
    void ranksAreSequentialStartingAtOne() {
        RoutePath a = path(600, 300, 0, 0, 0.9);
        RoutePath b = path(900, 200, 0, 0, 0.9);
        RoutePath c = path(1200, 100, 0, 0, 0.9);
        List<RankedRoute> ranked = ranker.rank(List.of(a, b, c),
                PreferenceProfile.forObjective(RankingObjective.FASTEST));
        assertThat(ranked).extracting(RankedRoute::rank).containsExactly(1, 2, 3);
    }
}
