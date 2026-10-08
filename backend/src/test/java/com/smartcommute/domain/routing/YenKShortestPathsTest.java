package com.smartcommute.domain.routing;

import com.smartcommute.domain.constraint.Constraints;
import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.Node;
import com.smartcommute.domain.graph.TransportGraph;
import com.smartcommute.domain.graph.TransportMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YenKShortestPathsTest {

    /**
     * Graph with three distinct 1->4 paths of increasing cost:
     *   via 3: 5+5   = 10
     *   via 2: 10+10 = 20
     *   direct:      = 100
     */
    private TransportGraph tripleDiamond() {
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
    void returnsKPathsInIncreasingCostOrder() {
        YenKShortestPaths yen = new YenKShortestPaths(tripleDiamond(),
                WeightFunction.byTravelTime(), Heuristic.zero());
        List<RoutePath> paths = yen.find(1, 4, 3, Constraints.none());

        assertThat(paths).hasSize(3);
        assertThat(paths.get(0).totalSeconds()).isEqualTo(10);
        assertThat(paths.get(1).totalSeconds()).isEqualTo(20);
        assertThat(paths.get(2).totalSeconds()).isEqualTo(100);
        // Non-decreasing order.
        assertThat(paths.get(0).totalSeconds()).isLessThanOrEqualTo(paths.get(1).totalSeconds());
        assertThat(paths.get(1).totalSeconds()).isLessThanOrEqualTo(paths.get(2).totalSeconds());
    }

    @Test
    void returnsFewerThanKWhenNotEnoughDistinctPaths() {
        TransportGraph twoNodes = TransportGraph.builder()
                .addNode(new Node(1, "S", new GeoPoint(0, 0), TransportMode.WALK))
                .addNode(new Node(2, "T", new GeoPoint(0, 0.01), TransportMode.WALK))
                .addEdge(new Edge(1, 1, 2, TransportMode.WALK, 60, 0, 100, false, 0.99))
                .build();
        YenKShortestPaths yen = new YenKShortestPaths(twoNodes, WeightFunction.byTravelTime(), Heuristic.zero());
        assertThat(yen.find(1, 2, 5, Constraints.none())).hasSize(1);
    }

    @Test
    void producedPathsAreLoopless() {
        YenKShortestPaths yen = new YenKShortestPaths(tripleDiamond(),
                WeightFunction.byTravelTime(), Heuristic.zero());
        for (RoutePath path : yen.find(1, 4, 3, Constraints.none())) {
            long distinctNodes = path.legs().stream()
                    .mapToLong(l -> l.fromNodeId()).distinct().count();
            // node count in a loopless path == number of legs (each from-node unique) ...
            assertThat(distinctNodes).isEqualTo(path.legs().size());
        }
    }

    @Test
    void costConstraintFiltersOutExpensivePaths() {
        // Path via 3 costs 0; add a costly parallel path and cap the budget.
        TransportGraph g = TransportGraph.builder()
                .addNode(new Node(1, "S", new GeoPoint(0, 0), TransportMode.WALK))
                .addNode(new Node(2, "A", new GeoPoint(0, 0.01), TransportMode.BUS))
                .addNode(new Node(3, "B", new GeoPoint(0, 0.005), TransportMode.WALK))
                .addNode(new Node(4, "T", new GeoPoint(0, 0.02), TransportMode.WALK))
                .addEdge(new Edge(12, 1, 2, TransportMode.BUS, 5, 5000, 10, false, 0.9))
                .addEdge(new Edge(24, 2, 4, TransportMode.BUS, 5, 5000, 10, false, 0.9))
                .addEdge(new Edge(13, 1, 3, TransportMode.WALK, 50, 0, 500, false, 0.99))
                .addEdge(new Edge(34, 3, 4, TransportMode.WALK, 50, 0, 500, false, 0.99))
                .build();
        YenKShortestPaths yen = new YenKShortestPaths(g, WeightFunction.byTravelTime(), Heuristic.zero());
        Constraints cheap = new Constraints(null, 100, null, java.util.Set.of()); // max 100 cents
        List<RoutePath> paths = yen.find(1, 4, 3, cheap);
        // Only the free WALK path qualifies under the budget.
        assertThat(paths).isNotEmpty();
        assertThat(paths).allMatch(p -> p.totalCostCents() <= 100);
    }
}
