package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.TransportGraph;

import java.util.List;

/**
 * The route engine contract (docs/04-lld.md section 2.4). Pure domain interface: no Spring, no
 * persistence, no HTTP — so it can be unit-tested in milliseconds.
 */
public interface RouteEngine {

    /**
     * Compute up to {@code k} feasible, ranked-by-weight candidate paths from source to target.
     *
     * @param graph       the transport graph
     * @param sourceId    source node id
     * @param targetId    target node id
     * @param k           maximum number of alternatives to return
     * @param constraints hard constraints; violating paths are never returned
     * @param weight      edge weight strategy (e.g. effective travel time)
     * @return up to k feasible paths, fewer if not that many exist; empty if none are feasible
     */
    List<RoutePath> kShortestConstrained(TransportGraph graph, long sourceId, long targetId,
                                         int k, Constraints constraints, WeightFunction weight);
}
