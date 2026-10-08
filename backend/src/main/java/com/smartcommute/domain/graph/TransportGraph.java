package com.smartcommute.domain.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An immutable, in-memory transport graph stored as an adjacency list.
 *
 * <p>Design decisions (see docs/05-database-design.md section 3.5 and docs/07-algorithms.md):</p>
 * <ul>
 *   <li>Built once and cached; static within a dataset version. Rebuilding per request would blow
 *       the latency budget (NFR-1).</li>
 *   <li>Immutable after construction, so it can be shared safely across request threads.</li>
 *   <li>Adjacency is a {@code Map<Long, List<Edge>>} for O(1) average neighbour lookup — the hot
 *       operation inside Dijkstra/A*.</li>
 *   <li>The {@link Builder} validates the graph (no dangling edges, non-negative weights) so the
 *       algorithms can assume a well-formed input.</li>
 * </ul>
 */
public final class TransportGraph {

    private final Map<Long, Node> nodes;
    private final Map<Long, List<Edge>> adjacency;
    private final int edgeCount;

    private TransportGraph(Map<Long, Node> nodes, Map<Long, List<Edge>> adjacency, int edgeCount) {
        this.nodes = nodes;
        this.adjacency = adjacency;
        this.edgeCount = edgeCount;
    }

    /** Outgoing edges from a node. Never null; empty for unknown/sink nodes. */
    public List<Edge> edgesFrom(long nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList());
    }

    public Node node(long id) {
        Node n = nodes.get(id);
        if (n == null) {
            throw new IllegalArgumentException("unknown node id: " + id);
        }
        return n;
    }

    public boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edgeCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Validated builder. Rejects malformed graphs early so the routing algorithms never have to
     * defend against bad input at runtime.
     */
    public static final class Builder {
        private final Map<Long, Node> nodes = new HashMap<>();
        private final List<Edge> edges = new ArrayList<>();

        public Builder addNode(Node node) {
            if (nodes.containsKey(node.id())) {
                throw new IllegalArgumentException("duplicate node id: " + node.id());
            }
            nodes.put(node.id(), node);
            return this;
        }

        public Builder addEdge(Edge edge) {
            edges.add(edge);
            return this;
        }

        public TransportGraph build() {
            Map<Long, List<Edge>> adjacency = new HashMap<>();
            for (Edge e : edges) {
                if (!nodes.containsKey(e.fromNodeId())) {
                    throw new IllegalArgumentException("edge " + e.id() + " has unknown fromNodeId " + e.fromNodeId());
                }
                if (!nodes.containsKey(e.toNodeId())) {
                    throw new IllegalArgumentException("edge " + e.id() + " has unknown toNodeId " + e.toNodeId());
                }
                adjacency.computeIfAbsent(e.fromNodeId(), k -> new ArrayList<>()).add(e);
            }
            // Freeze the adjacency lists to keep the graph effectively immutable.
            Map<Long, List<Edge>> frozen = new HashMap<>(adjacency.size());
            for (Map.Entry<Long, List<Edge>> entry : adjacency.entrySet()) {
                frozen.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
            }
            return new TransportGraph(
                    Collections.unmodifiableMap(new HashMap<>(nodes)),
                    Collections.unmodifiableMap(frozen),
                    edges.size());
        }
    }
}
