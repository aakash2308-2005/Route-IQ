package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.TransportGraph;

import java.util.List;

/**
 * Default {@link RouteEngine} implementation: A*-based shortest path wrapped by Yen's k-shortest
 * algorithm (docs/07-algorithms.md sections 3 and 4).
 *
 * <p>A geographic A* heuristic is used when a positive max speed is supplied, which prunes the
 * search toward the known target without sacrificing optimality. With a zero heuristic this reduces
 * to plain Dijkstra.</p>
 */
public final class DijkstraKShortestEngine implements RouteEngine {

    /** Upper bound on any edge speed in the sample network (m/s). Keeps the A* heuristic admissible. */
    private final double maxSpeedMetersPerSecond;
    private final boolean useAStar;

    public DijkstraKShortestEngine(double maxSpeedMetersPerSecond, boolean useAStar) {
        this.maxSpeedMetersPerSecond = maxSpeedMetersPerSecond;
        this.useAStar = useAStar;
    }

    /** Default engine: A* enabled with a 40 m/s ceiling (~144 km/h, safely above metro speeds). */
    public DijkstraKShortestEngine() {
        this(40.0, true);
    }

    @Override
    public List<RoutePath> kShortestConstrained(TransportGraph graph, long sourceId, long targetId,
                                                int k, Constraints constraints, WeightFunction weight) {
        Heuristic heuristic = useAStar
                ? Heuristic.haversineTime(graph, maxSpeedMetersPerSecond)
                : Heuristic.zero();
        YenKShortestPaths yen = new YenKShortestPaths(graph, weight, heuristic);
        return yen.find(sourceId, targetId, k, constraints);
    }
}
