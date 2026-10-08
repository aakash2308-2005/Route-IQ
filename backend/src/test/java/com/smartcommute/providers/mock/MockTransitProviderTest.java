package com.smartcommute.providers.mock;

import com.smartcommute.domain.graph.Edge;
import com.smartcommute.providers.port.NetworkSnapshot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockTransitProviderTest {

    private final MockTransitProvider provider = new MockTransitProvider();

    @Test
    void loadsSampleCityWithNodesAndEdges() {
        NetworkSnapshot snapshot = provider.loadNetwork("sample-city");
        assertThat(snapshot.cityId()).isEqualTo("sample-city");
        assertThat(snapshot.nodes()).isNotEmpty();
        assertThat(snapshot.edges()).isNotEmpty();
    }

    @Test
    void expandsBidirectionalTravelEdges() {
        NetworkSnapshot snapshot = provider.loadNetwork("sample-city");
        // Edge 1000 (Central metro -> Park metro) is bidirectional, so a reverse edge must exist.
        boolean forward = snapshot.edges().stream()
                .anyMatch(e -> e.fromNodeId() == 2 && e.toNodeId() == 11 && !e.isTransfer());
        boolean reverse = snapshot.edges().stream()
                .anyMatch(e -> e.fromNodeId() == 11 && e.toNodeId() == 2 && !e.isTransfer());
        assertThat(forward).isTrue();
        assertThat(reverse).isTrue();
    }

    @Test
    void generatesTransferEdgesBetweenLayersAtSameStop() {
        NetworkSnapshot snapshot = provider.loadNetwork("sample-city");
        // Central has WALK(1), METRO(2), BUS(3): transfer edges must connect them.
        long transfersAtCentral = snapshot.edges().stream()
                .filter(Edge::isTransfer)
                .filter(e -> (e.fromNodeId() == 1 || e.fromNodeId() == 2 || e.fromNodeId() == 3))
                .filter(e -> (e.toNodeId() == 1 || e.toNodeId() == 2 || e.toNodeId() == 3))
                .count();
        // 3 layers -> 3*2 = 6 directed transfer edges.
        assertThat(transfersAtCentral).isEqualTo(6);
    }

    @Test
    void rejectsUnknownCity() {
        assertThatThrownBy(() -> provider.loadNetwork("atlantis"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
