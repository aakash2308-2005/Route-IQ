package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.Node;
import com.smartcommute.domain.graph.TransportGraph;
import com.smartcommute.domain.graph.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ShortestPathTest {

    /**
     * Builds a small directed graph:
     *   1 --(10)--> 2 --(10)--> 4
     *   1 --(5)---> 3 --(5)---> 4
     *   1 --(100)-> 4  (direct but expensive)
     * Shortest 1->4 by weight is via node 3 (5+5=10).
     */
    private TransportGraph diamond() {
        return TransportGraph.builder()
                .addNode(new Node(1, "S", new GeoPoint(0, 0), TransportMode.WALK))
                .addNode(new Node(2, "A", new GeoPoint(0, 0.01), TransportMode.WALK))
                .addNode(new Node(3, "B", new GeoPoint(0, 0.005), TransportMode.WALK))
                .addNode(new Node(4, "T", new GeoPoint(0, 0.02), TransportMode.WALK))
                .addEdge(new Edge(12, 1, 2, TransportMode.WALK, 10, 0, 10, false, 0.99))
                .addEdge(new Edge(24, 2, 4, TransportMode.WALK, 10, 0, 10, false, 0.99))
                .addEdge(new Edge(13, 1, 3, TransportMode.WALK, 5, 0, 5, false, 0.99))
                .addEdge(new Edge(34, 3, 4, TransportMode.WALK, 5, 0, 5, false, 0.99))
                .addEdge(new Edge(14, 1, 4, TransportMode.WALK, 100, 0, 100, false, 0.99))
                .build();
    }

    @Test
    void dijkstraFindsMinimumWeightPath() {
        TransportGraph g = diamond();
        ShortestPath sp = ShortestPath.dijkstra(g, WeightFunction.byTravelTime());
        Optional<RoutePath> path = sp.find(1, 4, Constraints.none());
        assertThat(path).isPresent();
        assertThat(path.get().totalSeconds()).isEqualTo(10); // via node 3
        assertThat(path.get().legs()).hasSize(2);
    }

    @Test
    void aStarProducesSameOptimalResultAsDijkstra() {
        TransportGraph g = diamond();
        ShortestPath dijkstra = ShortestPath.dijkstra(g, WeightFunction.byTravelTime());
        ShortestPath astar = new ShortestPath(g, WeightFunction.byTravelTime(),
                Heuristic.haversineTime(g, 40.0));
        int dijkstraCost = dijkstra.find(1, 4, Constraints.none()).orElseThrow().totalSeconds();
        int astarCost = astar.find(1, 4, Constraints.none()).orElseThrow().totalSeconds();
        assertThat(astarCost).isEqualTo(dijkstraCost);
    }

    @Test
    void sourceEqualsTargetGivesEmptyPath() {
        ShortestPath sp = ShortestPath.dijkstra(diamond(), WeightFunction.byTravelTime());
        RoutePath path = sp.find(1, 1, Constraints.none()).orElseThrow();
        assertThat(path.legs()).isEmpty();
        assertThat(path.totalSeconds()).isZero();
    }

    @Test
    void unreachableTargetReturnsEmpty() {
        TransportGraph g = TransportGraph.builder()
                .addNode(new Node(1, "S", new GeoPoint(0, 0), TransportMode.WALK))
                .addNode(new Node(2, "T", new GeoPoint(0, 0.01), TransportMode.WALK))
                .build(); // no edges
        ShortestPath sp = ShortestPath.dijkstra(g, WeightFunction.byTravelTime());
        assertThat(sp.find(1, 2, Constraints.none())).isEmpty();
    }

    @Test
    void modeRestrictionBlocksDisallowedEdges() {
        TransportGraph g = TransportGraph.builder()
                .addNode(new Node(1, "S", new GeoPoint(0, 0), TransportMode.WALK))
                .addNode(new Node(2, "T", new GeoPoint(0, 0.01), TransportMode.BUS))
                .addEdge(new Edge(1, 1, 2, TransportMode.BUS, 60, 100, 500, false, 0.9))
                .build();
        ShortestPath sp = ShortestPath.dijkstra(g, WeightFunction.byTravelTime());
        // Only WALK allowed -> the BUS edge cannot be used -> unreachable.
        Constraints walkOnly = new Constraints(null, null, null, java.util.Set.of(TransportMode.WALK));
        assertThat(sp.find(1, 2, walkOnly)).isEmpty();
    }
}
