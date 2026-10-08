package com.smartcommute.config;

import com.smartcommute.domain.ranking.RankingStrategyFactory;
import com.smartcommute.domain.ranking.RouteRanker;
import com.smartcommute.domain.routing.DijkstraKShortestEngine;
import com.smartcommute.domain.routing.RouteEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pure-domain components as Spring beans. The domain classes themselves have no Spring
 * dependency (that's the whole point of the layering, docs/02-architecture.md); this config is the
 * single place where the application decides which concrete implementations to use.
 */
@Configuration
public class DomainConfig {

    @Bean
    public RouteEngine routeEngine() {
        // A* enabled with a conservative max-speed ceiling to keep the heuristic admissible.
        return new DijkstraKShortestEngine();
    }

    @Bean
    public RankingStrategyFactory rankingStrategyFactory() {
        return new RankingStrategyFactory();
    }

    @Bean
    public RouteRanker routeRanker(RankingStrategyFactory factory) {
        return new RouteRanker(factory);
    }
}
