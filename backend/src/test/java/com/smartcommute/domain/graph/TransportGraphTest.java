package com.smartcommute.domain.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransportGraphTest {

    private static Node walkNode(long id, String name) {
        return new Node(id, name, new GeoPoint(12.0, 77.0), TransportMode.WALK);
    }

    @Test
    void buildsAdjacencyAndLooksUpNodes() {
        TransportGraph g = TransportGraph.builder()
                .addNode(walkNode(1, "A"))
                .addNode(walkNode(2, "B"))
                .addEdge(new Edge(100, 1, 2, TransportMode.WALK, 60, 0, 100, false, 0.99))
                .build();

        assertThat(g.nodeCount()).isEqualTo(2);
        assertThat(g.edgeCount()).isEqualTo(1);
        assertThat(g.edgesFrom(1)).hasSize(1);
        assertThat(g.edgesFrom(2)).isEmpty();
        assertThat(g.node(1).stopName()).isEqualTo("A");
    }

    @Test
    void rejectsDanglingEdge() {
        assertThatThrownBy(() -> TransportGraph.builder()
                .addNode(walkNode(1, "A"))
                .addEdge(new Edge(100, 1, 999, TransportMode.WALK, 60, 0, 100, false, 0.99))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown toNodeId");
    }

    @Test
    void rejectsDuplicateNodeId() {
        assertThatThrownBy(() -> TransportGraph.builder()
                .addNode(walkNode(1, "A"))
                .addNode(walkNode(1, "B")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate node id");
    }

    @Test
    void edgeRejectsNegativeWeights() {
        assertThatThrownBy(() -> new Edge(1, 1, 2, TransportMode.WALK, -1, 0, 0, false, 0.99))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Edge(1, 1, 2, TransportMode.WALK, 10, -5, 0, false, 0.99))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownNodeLookupThrows() {
        TransportGraph g = TransportGraph.builder().addNode(walkNode(1, "A")).build();
        assertThatThrownBy(() -> g.node(42)).isInstanceOf(IllegalArgumentException.class);
    }
}
