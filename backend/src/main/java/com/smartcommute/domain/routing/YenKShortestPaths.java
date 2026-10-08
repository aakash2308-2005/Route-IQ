package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.TransportGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Yen's algorithm for loopless K-shortest paths (docs/07-algorithms.md section 4).
 *
 * <p>Yen builds the next shortest path from the ones already found. For each already-accepted path
 * and each "spur node" along it, it:</p>
 * <ol>
 *   <li>removes edges that would recreate an already-found prefix (so we get a genuinely different
 *       path), and removes the root nodes (to keep the result loopless);</li>
 *   <li>computes a shortest "spur path" from the spur node to the target;</li>
 *   <li>joins the root path (source -> spur) with the spur path to form a candidate, stored in a
 *       min-heap of candidates {@code B};</li>
 * </ol>
 * The best candidate from {@code B} becomes the next accepted path. We keep pulling until we have
 * K <em>feasible</em> paths (those satisfying the hard {@link Constraints}) or candidates run out.
 *
 * <p>This implementation works on ordered {@link Edge} lists (via
 * {@link ShortestPath#findEdges}) rather than reconstructing edges from legs, which keeps it correct
 * even when multiple edges connect the same node pair.</p>
 *
 * <p>Feasibility is checked as paths are accepted rather than only pruned during search, because
 * walking distance / cost / transfer count are path-cumulative and cannot always be pruned
 * edge-locally without discarding paths that would become feasible (docs/07 section 4.4).</p>
 */
public final class YenKShortestPaths {

    private final WeightFunction weight;
    private final ShortestPath shortestPath;

    public YenKShortestPaths(TransportGraph graph, WeightFunction weight, Heuristic heuristic) {
        this.weight = weight;
        this.shortestPath = new ShortestPath(graph, weight, heuristic);
    }

    /**
     * Returns up to {@code k} feasible, distinct, loopless paths ordered by total search weight.
     * May return fewer than k if the graph does not contain that many feasible distinct paths.
     */
    public List<RoutePath> find(long source, long target, int k, Constraints constraints) {
        if (k <= 0) {
            return List.of();
        }

        Optional<List<Edge>> firstOpt = shortestPath.findEdges(source, target, constraints,
                Set.of(), Set.of());
        if (firstOpt.isEmpty()) {
            return List.of();
        }

        // All paths accepted so far, as edge lists (used to build spurs). Ordered by increasing weight.
        List<List<Edge>> accepted = new ArrayList<>();
        accepted.add(firstOpt.get());

        // Candidate heap ordered by total weight; dedup by node sequence.
        PriorityQueue<Candidate> candidates =
                new PriorityQueue<>(Comparator.comparingDouble(c -> c.totalWeight));
        Set<List<Long>> seenSequences = new HashSet<>();
        seenSequences.add(nodeSequence(source, firstOpt.get()));

        List<RoutePath> feasible = new ArrayList<>();
        addIfFeasible(feasible, firstOpt.get(), constraints);

        while (feasible.size() < k) {
            List<Edge> lastPath = accepted.get(accepted.size() - 1);

            for (int i = 0; i < lastPath.size(); i++) {
                long spurNode = lastPath.get(i).fromNodeId();
                List<Edge> rootEdges = lastPath.subList(0, i);

                Set<Long> bannedEdges = new HashSet<>();
                Set<Long> bannedNodes = new HashSet<>();

                // Ban the edge that continues any previously accepted path sharing this root.
                for (List<Edge> p : accepted) {
                    if (sharesRoot(p, rootEdges) && p.size() > i) {
                        bannedEdges.add(p.get(i).id());
                    }
                }
                // Ban root nodes (their from-nodes) to keep the path loopless.
                for (Edge e : rootEdges) {
                    bannedNodes.add(e.fromNodeId());
                }

                Optional<List<Edge>> spur =
                        shortestPath.findEdges(spurNode, target, constraints, bannedEdges, bannedNodes);
                if (spur.isEmpty()) {
                    continue;
                }
                List<Edge> totalEdges = new ArrayList<>(rootEdges);
                totalEdges.addAll(spur.get());

                List<Long> seq = nodeSequence(source, totalEdges);
                if (!seenSequences.contains(seq) && !hasLoop(seq)) {
                    seenSequences.add(seq);
                    candidates.add(new Candidate(totalEdges, totalWeight(totalEdges)));
                }
            }

            if (candidates.isEmpty()) {
                break;
            }
            Candidate next = candidates.poll();
            accepted.add(next.edges);
            addIfFeasible(feasible, next.edges, constraints);
        }
        return feasible;
    }

    private void addIfFeasible(List<RoutePath> feasible, List<Edge> edges, Constraints constraints) {
        RoutePath path = ShortestPath.toRoutePath(edges);
        if (path.satisfies(constraints)) {
            feasible.add(path);
        }
    }

    private double totalWeight(List<Edge> edges) {
        double total = 0.0;
        for (Edge e : edges) {
            total += weight.weight(e);
        }
        return total;
    }

    private static boolean sharesRoot(List<Edge> pathEdges, List<Edge> rootEdges) {
        if (pathEdges.size() < rootEdges.size()) {
            return false;
        }
        for (int i = 0; i < rootEdges.size(); i++) {
            if (pathEdges.get(i).id() != rootEdges.get(i).id()) {
                return false;
            }
        }
        return true;
    }

    private static List<Long> nodeSequence(long source, List<Edge> edges) {
        List<Long> seq = new ArrayList<>(edges.size() + 1);
        seq.add(source);
        for (Edge e : edges) {
            seq.add(e.toNodeId());
        }
        return seq;
    }

    private static boolean hasLoop(List<Long> nodeSequence) {
        return new LinkedHashSet<>(nodeSequence).size() != nodeSequence.size();
    }

    private record Candidate(List<Edge> edges, double totalWeight) {
    }
}
