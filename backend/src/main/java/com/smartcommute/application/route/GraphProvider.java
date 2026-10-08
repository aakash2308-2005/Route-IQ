package com.smartcommute.application.route;

import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.Node;
import com.smartcommute.domain.graph.TransportGraph;
import com.smartcommute.providers.port.NetworkSnapshot;
import com.smartcommute.providers.port.TransitProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds and caches the in-memory {@link TransportGraph} for the configured city.
 *
 * <p>The graph is static within a dataset version, so it is built lazily once and reused across all
 * requests (docs/02-architecture.md section 7). This is the single most important cache for meeting
 * the latency budget (NFR-1) — rebuilding the graph per request would be far too slow.</p>
 *
 * <p>Thread-safety: the graph is immutable once built; construction is guarded so concurrent first
 * requests don't build it twice.</p>
 */
@Component
public class GraphProvider {

    private static final Logger log = LoggerFactory.getLogger(GraphProvider.class);

    private final TransitProvider transitProvider;
    private final String cityId;

    private volatile TransportGraph cachedGraph;
    private volatile NetworkSnapshot cachedSnapshot;

    public GraphProvider(TransitProvider transitProvider,
                         @Value("${smartcommute.city.id:sample-city}") String cityId) {
        this.transitProvider = transitProvider;
        this.cityId = cityId;
    }

    public TransportGraph graph() {
        TransportGraph local = cachedGraph;
        if (local == null) {
            synchronized (this) {
                if (cachedGraph == null) {
                    build();
                }
                local = cachedGraph;
            }
        }
        return local;
    }

    public NetworkSnapshot snapshot() {
        graph(); // ensure loaded
        return cachedSnapshot;
    }

    private void build() {
        NetworkSnapshot snapshot = transitProvider.loadNetwork(cityId);
        TransportGraph.Builder builder = TransportGraph.builder();
        for (Node n : snapshot.nodes()) {
            builder.addNode(n);
        }
        for (Edge e : snapshot.edges()) {
            builder.addEdge(e);
        }
        this.cachedSnapshot = snapshot;
        this.cachedGraph = builder.build();
        log.info("Built transport graph for city={} version={} nodes={} edges={}",
                cityId, snapshot.datasetVersion(), cachedGraph.nodeCount(), cachedGraph.edgeCount());
    }
}
