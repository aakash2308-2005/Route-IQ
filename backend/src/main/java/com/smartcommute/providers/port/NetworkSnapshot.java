package com.smartcommute.providers.port;

import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.Node;

import java.util.List;

/**
 * A point-in-time snapshot of a city's transport network as returned by a {@link TransitProvider}.
 * Shaped to be GTFS-ready: a real GTFS adapter would translate stops/trips into nodes/edges here
 * without the route engine ever knowing the source format.
 */
public record NetworkSnapshot(String cityId, String datasetVersion, List<Node> nodes, List<Edge> edges) {

    public NetworkSnapshot {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}
