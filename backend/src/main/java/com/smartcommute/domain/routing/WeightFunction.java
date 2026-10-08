package com.smartcommute.domain.routing;

import com.smartcommute.domain.graph.Edge;

/**
 * Strategy for computing the search weight of an edge (docs/04-lld.md section 2.4).
 *
 * <p>This is the Strategy pattern applied to the cost that the shortest-path search minimises.
 * By swapping the weight function we can optimise the graph for different metrics (pure time, or a
 * blended metric) and inject dynamic factors (traffic multiplier, ML ETA) <em>without changing the
 * algorithm</em>. The default optimises for effective travel time.</p>
 */
@FunctionalInterface
public interface WeightFunction {

    /** The non-negative weight used by Dijkstra/A* for this edge. */
    double weight(Edge edge);

    /** Default: minimise base travel time in seconds. */
    static WeightFunction byTravelTime() {
        return edge -> edge.baseTravelSeconds();
    }
}
