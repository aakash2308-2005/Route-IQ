package com.smartcommute.domain.routing;

import com.smartcommute.domain.graph.TransportGraph;

/**
 * Admissible heuristic for A* search: an estimated lower bound on the remaining weight from a node
 * to the target (docs/07-algorithms.md section 3.5).
 *
 * <p>"Admissible" means the estimate never overestimates the true remaining cost; this guarantees
 * A* still returns an optimal path. Dijkstra is simply A* with a zero heuristic.</p>
 */
@FunctionalInterface
public interface Heuristic {

    /** Estimated remaining weight from {@code nodeId} to {@code targetId}. Must be >= 0. */
    double estimate(long nodeId, long targetId);

    /** Zero heuristic — turns A* into plain Dijkstra. */
    static Heuristic zero() {
        return (nodeId, targetId) -> 0.0;
    }

    /**
     * Geographic heuristic for time-weighted search: straight-line (haversine) distance divided by
     * a max plausible speed gives a lower bound on remaining travel <em>seconds</em>. Admissible as
     * long as {@code maxSpeedMetersPerSecond} is at least the fastest edge speed in the graph.
     */
    static Heuristic haversineTime(TransportGraph graph, double maxSpeedMetersPerSecond) {
        if (maxSpeedMetersPerSecond <= 0) {
            throw new IllegalArgumentException("maxSpeedMetersPerSecond must be > 0");
        }
        return (nodeId, targetId) -> {
            double meters = graph.node(nodeId).location().haversineMeters(graph.node(targetId).location());
            return meters / maxSpeedMetersPerSecond;
        };
    }
}
