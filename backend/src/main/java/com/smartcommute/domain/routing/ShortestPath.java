package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.TransportGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Single-source single-target shortest path via Dijkstra / A* (docs/07-algorithms.md section 3).
 *
 * <p>Implementation notes:</p>
 * <ul>
 *   <li><b>Priority queue with lazy deletion:</b> Java's {@link PriorityQueue} has no decrease-key.
 *       Instead of updating an existing entry we push a new one and skip stale entries when popped
 *       ({@code current.dist > best[node]}). This keeps the code simple and stays
 *       O((V+E) log V).</li>
 *   <li><b>A*:</b> the frontier is ordered by {@code g + h} where {@code g} is the known distance
 *       and {@code h} is the {@link Heuristic}. With {@link Heuristic#zero()} this is exactly
 *       Dijkstra.</li>
 *   <li><b>Mode filtering:</b> edges of disallowed modes are skipped during expansion, which is the
 *       correct place to enforce edge-local hard constraints (docs/07 section 4.4).</li>
 *   <li><b>Banned edges/nodes:</b> supplied by Yen's algorithm to force alternative paths.</li>
 * </ul>
 */
public final class ShortestPath {

    private static final double INF = Double.POSITIVE_INFINITY;

    private final TransportGraph graph;
    private final WeightFunction weight;
    private final Heuristic heuristic;

    public ShortestPath(TransportGraph graph, WeightFunction weight, Heuristic heuristic) {
        this.graph = graph;
        this.weight = weight;
        this.heuristic = heuristic;
    }

    /** Convenience factory for plain Dijkstra (zero heuristic). */
    public static ShortestPath dijkstra(TransportGraph graph, WeightFunction weight) {
        return new ShortestPath(graph, weight, Heuristic.zero());
    }

    /**
     * Computes the least-weight path from {@code source} to {@code target}.
     *
     * @param bannedEdgeIds edges that must not be used (Yen spur construction)
     * @param bannedNodeIds nodes that must not be visited (Yen root-node removal)
     * @return the path if reachable under the constraints, otherwise empty
     */
    public Optional<RoutePath> find(long source, long target, Constraints constraints,
                                    Set<Long> bannedEdgeIds, Set<Long> bannedNodeIds) {
        return findEdges(source, target, constraints, bannedEdgeIds, bannedNodeIds)
                .map(ShortestPath::toRoutePath);
    }

    /**
     * Like {@link #find}, but returns the ordered list of {@link Edge}s that make up the path. Yen's
     * algorithm consumes this directly so it never has to re-derive edges from legs (which would be
     * fragile when multiple edges connect the same node pair).
     */
    public Optional<List<Edge>> findEdges(long source, long target, Constraints constraints,
                                          Set<Long> bannedEdgeIds, Set<Long> bannedNodeIds) {
        if (!graph.hasNode(source) || !graph.hasNode(target)) {
            return Optional.empty();
        }
        if (source == target) {
            return Optional.of(List.of());
        }
        if (bannedNodeIds.contains(source) || bannedNodeIds.contains(target)) {
            return Optional.empty();
        }

        Map<Long, Double> gScore = new HashMap<>();   // best known distance to a node
        Map<Long, Edge> prevEdge = new HashMap<>();    // edge used to reach a node (for reconstruction)
        gScore.put(source, 0.0);

        PriorityQueue<FrontierEntry> frontier =
                new PriorityQueue<>((a, b) -> Double.compare(a.fScore, b.fScore));
        frontier.add(new FrontierEntry(source, 0.0, heuristic.estimate(source, target)));

        while (!frontier.isEmpty()) {
            FrontierEntry current = frontier.poll();
            // Skip stale heap entries (lazy deletion).
            if (current.gScore > gScore.getOrDefault(current.node, INF)) {
                continue;
            }
            if (current.node == target) {
                return Optional.of(reconstructEdges(prevEdge, source, target));
            }
            for (Edge edge : graph.edgesFrom(current.node)) {
                if (bannedEdgeIds.contains(edge.id())) {
                    continue;
                }
                if (bannedNodeIds.contains(edge.toNodeId())) {
                    continue;
                }
                if (!constraints.allowsMode(edge.mode())) {
                    continue;
                }
                double tentativeG = current.gScore + weight.weight(edge);
                if (tentativeG < gScore.getOrDefault(edge.toNodeId(), INF)) {
                    gScore.put(edge.toNodeId(), tentativeG);
                    prevEdge.put(edge.toNodeId(), edge);
                    double f = tentativeG + heuristic.estimate(edge.toNodeId(), target);
                    frontier.add(new FrontierEntry(edge.toNodeId(), tentativeG, f));
                }
            }
        }
        return Optional.empty();
    }

    /** Overload without banned sets. */
    public Optional<RoutePath> find(long source, long target, Constraints constraints) {
        return find(source, target, constraints, Collections.emptySet(), Collections.emptySet());
    }

    /** Rebuilds the ordered edge list by walking predecessor edges from target back to source. */
    private List<Edge> reconstructEdges(Map<Long, Edge> prevEdge, long source, long target) {
        List<Edge> edgesReversed = new ArrayList<>();
        long node = target;
        while (node != source) {
            Edge edge = prevEdge.get(node);
            if (edge == null) {
                // Should not happen if target was reached, but guard defensively.
                return List.of();
            }
            edgesReversed.add(edge);
            node = edge.fromNodeId();
        }
        Collections.reverse(edgesReversed);
        return edgesReversed;
    }

    /** Converts an ordered edge list to a RoutePath, computing reliability as the product of legs. */
    static RoutePath toRoutePath(List<Edge> edges) {
        List<Leg> legs = new ArrayList<>(edges.size());
        double reliability = 1.0;
        for (Edge e : edges) {
            legs.add(new Leg(e.mode(), e.fromNodeId(), e.toNodeId(),
                    e.baseTravelSeconds(), e.costCents(), e.distanceMeters(), e.isTransfer()));
            reliability *= e.reliabilityScore();
        }
        return RoutePath.of(legs, edges.isEmpty() ? 1.0 : reliability);
    }

    /** Frontier entry ordered by f = g + h. */
    private record FrontierEntry(long node, double gScore, double fScore) {
    }
}
