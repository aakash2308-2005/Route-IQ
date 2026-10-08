package com.smartcommute.application.route;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.constraint.PreferenceProfile;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportGraph;
import com.smartcommute.domain.ranking.RankedRoute;
import com.smartcommute.domain.ranking.RouteRanker;
import com.smartcommute.domain.routing.Leg;
import com.smartcommute.domain.routing.RouteEngine;
import com.smartcommute.domain.routing.RoutePath;
import com.smartcommute.domain.routing.WeightFunction;
import com.smartcommute.application.history.TripHistoryService;
import com.smartcommute.application.user.PreferenceService;
import com.smartcommute.persistence.entity.PreferenceEntity;
import com.smartcommute.providers.port.EtaProvider;
import com.smartcommute.providers.port.MapProvider;
import com.smartcommute.web.error.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates a route-planning request (docs/03-hld.md section 3.2). This is the application-layer
 * "use case" that ties together preferences, caching, providers, the pure-domain route engine, and
 * the ranker. It contains no algorithm code itself — that lives in the domain.
 *
 * <p>Flow: effective constraints -> cache lookup -> resolve endpoints -> build a traffic/ETA-aware
 * weight function -> engine.kShortestConstrained -> rank -> map to {@link RouteResult} -> cache +
 * async history.</p>
 */
@Service
public class RoutePlanningService {

    private static final Logger log = LoggerFactory.getLogger(RoutePlanningService.class);

    private final GraphProvider graphProvider;
    private final MapProvider mapProvider;
    private final EtaProvider etaProvider;
    private final RouteEngine routeEngine;
    private final RouteRanker routeRanker;
    private final RouteResultCache cache;
    private final PreferenceService preferenceService;
    private final TripHistoryService tripHistoryService;

    public RoutePlanningService(GraphProvider graphProvider, MapProvider mapProvider, EtaProvider etaProvider,
                                RouteEngine routeEngine, RouteRanker routeRanker, RouteResultCache cache,
                                PreferenceService preferenceService, TripHistoryService tripHistoryService) {
        this.graphProvider = graphProvider;
        this.mapProvider = mapProvider;
        this.etaProvider = etaProvider;
        this.routeEngine = routeEngine;
        this.routeRanker = routeRanker;
        this.cache = cache;
        this.preferenceService = preferenceService;
        this.tripHistoryService = tripHistoryService;
    }

    /** Result plus whether it came from cache (for the X-Cache header). */
    public record PlanOutcome(List<RouteResult> routes, boolean fromCache, String note) {
    }

    public PlanOutcome plan(PlanRouteCommand cmd) {
        // 1. Cache lookup (fail-open).
        var cached = cache.get(cmd);
        if (cached.isPresent()) {
            return new PlanOutcome(cached.get(), true, cached.get().isEmpty() ? emptyNote() : null);
        }

        // 2. Merge saved defaults with per-request overrides into effective constraints & prefs.
        PreferenceEntity defaults = preferenceService.getOrDefaults(cmd.userId());
        Constraints constraints = effectiveConstraints(cmd, defaults);
        RankingObjective objective = cmd.objective() != null ? cmd.objective() : defaults.getObjective();
        PreferenceProfile prefs = preferenceService.toProfile(defaults, objective);

        // 3. Resolve endpoints to graph nodes.
        long sourceId = mapProvider.resolveToNode(cmd.source())
                .orElseThrow(() -> new BadRequestException("could not resolve source: " + cmd.source()));
        long targetId = mapProvider.resolveToNode(cmd.destination())
                .orElseThrow(() -> new BadRequestException("could not resolve destination: " + cmd.destination()));

        // 4. Traffic/ETA-aware weight function: search minimises effective travel seconds.
        TransportGraph graph = graphProvider.graph();
        WeightFunction weight = edge -> {
            EtaProvider.EtaEstimate est =
                    etaProvider.estimate(edge.id(), edge.mode(), edge.baseTravelSeconds(), cmd.effectiveTime());
            return est.etaSeconds();
        };

        // 5. Constrained k-shortest search (pure domain).
        List<RoutePath> paths = routeEngine.kShortestConstrained(
                graph, sourceId, targetId, cmd.k(), constraints, weight);

        // 6. Rank.
        List<RankedRoute> ranked = routeRanker.rank(paths, prefs);

        // 7. Map to API results (resolve node ids to stop names).
        List<RouteResult> results = new ArrayList<>(ranked.size());
        for (RankedRoute rr : ranked) {
            results.add(toResult(rr, graph));
        }

        // 8. Cache + async history (history only when we actually produced routes).
        cache.put(cmd, results);
        if (!results.isEmpty()) {
            RouteResult best = results.get(0);
            tripHistoryService.recordAsync(cmd.userId(), cmd.source(), cmd.destination(), objective,
                    best.totalSeconds(), best.totalCostCents(), best.totalWalkingMeters(), best.transferCount());
        }

        String note = results.isEmpty() ? emptyNote() : null;
        log.debug("planned route source={} dest={} objective={} results={}",
                cmd.source(), cmd.destination(), objective, results.size());
        return new PlanOutcome(results, false, note);
    }

    private Constraints effectiveConstraints(PlanRouteCommand cmd, PreferenceEntity defaults) {
        // Per-request overrides win over saved defaults (FR-17). A null override falls back to the default.
        Integer maxWalk = cmd.maxWalkingMeters() != null ? cmd.maxWalkingMeters() : defaults.getMaxWalkingMeters();
        Integer maxCost = cmd.maxCostCents() != null ? cmd.maxCostCents() : defaults.getMaxCostCents();
        Integer maxTransfers = cmd.maxTransfers() != null ? cmd.maxTransfers() : defaults.getMaxTransfers();
        return new Constraints(maxWalk, maxCost, maxTransfers,
                cmd.allowedModes() != null ? cmd.allowedModes() : java.util.Set.of());
    }

    private RouteResult toResult(RankedRoute rr, TransportGraph graph) {
        RoutePath path = rr.path();
        List<RouteResult.LegView> legViews = new ArrayList<>(path.legs().size());
        for (Leg leg : path.legs()) {
            legViews.add(new RouteResult.LegView(
                    leg.mode(),
                    stopName(graph, leg.fromNodeId()),
                    stopName(graph, leg.toNodeId()),
                    leg.travelSeconds(),
                    leg.costCents(),
                    leg.distanceMeters(),
                    leg.isTransfer()));
        }
        return new RouteResult(
                rr.rank(),
                path.totalSeconds(),
                path.totalCostCents(),
                path.totalWalkingMeters(),
                path.transferCount(),
                path.reliability(),
                rr.score().total(),
                rr.score().breakdown(),
                legViews);
    }

    private String stopName(TransportGraph graph, long nodeId) {
        return graph.hasNode(nodeId) ? graph.node(nodeId).stopName() : String.valueOf(nodeId);
    }

    private String emptyNote() {
        return "No route satisfies the given constraints (try increasing max walking distance or budget).";
    }
}
