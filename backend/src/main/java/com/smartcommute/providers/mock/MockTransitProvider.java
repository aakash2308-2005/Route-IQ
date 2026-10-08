package com.smartcommute.providers.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommute.domain.graph.Edge;
import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.Node;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.providers.port.NetworkSnapshot;
import com.smartcommute.providers.port.TransitProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock transit provider (default {@code mock} profile). Loads the bundled synthetic city dataset
 * and expands it into the domain graph:
 *
 * <ul>
 *   <li>Copies raw nodes (per-mode layer nodes) directly.</li>
 *   <li>Expands {@code bidirectional} travel edges into two directed edges.</li>
 *   <li>Generates transfer edges between every pair of layers at the same physical stop, in both
 *       directions, using that stop's transfer penalty (docs/07-algorithms.md section 2.1).</li>
 * </ul>
 *
 * <p>This translation is exactly what a real GTFS adapter would also do — the engine only ever sees
 * {@link NetworkSnapshot}.</p>
 */
@Component
@Profile("mock")
public class MockTransitProvider implements TransitProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicLong syntheticEdgeId = new AtomicLong(900_000);

    @Override
    public NetworkSnapshot loadNetwork(String cityId) {
        SampleCityData data = load();
        if (!data.cityId.equals(cityId)) {
            throw new IllegalArgumentException("unknown city: " + cityId);
        }

        Map<String, SampleCityData.Stop> stopByName = new HashMap<>();
        for (SampleCityData.Stop s : data.stops) {
            stopByName.put(s.name, s);
        }

        List<Node> nodes = new ArrayList<>();
        // stop name -> layer -> nodeId, used to build transfer edges.
        Map<String, Map<TransportMode, Long>> layersByStop = new HashMap<>();
        for (SampleCityData.RawNode rn : data.nodes) {
            SampleCityData.Stop stop = stopByName.get(rn.stop);
            if (stop == null) {
                throw new IllegalStateException("node " + rn.id + " references unknown stop " + rn.stop);
            }
            TransportMode layer = TransportMode.valueOf(rn.layer);
            nodes.add(new Node(rn.id, rn.stop, new GeoPoint(stop.lat, stop.lon), layer));
            layersByStop.computeIfAbsent(rn.stop, k -> new HashMap<>()).put(layer, rn.id);
        }

        List<Edge> edges = new ArrayList<>();
        // Travel edges (expand bidirectional).
        for (SampleCityData.RawEdge re : data.edges) {
            TransportMode mode = TransportMode.valueOf(re.mode);
            edges.add(new Edge(re.id, re.from, re.to, mode, re.seconds, re.costCents, re.meters, false, re.reliability));
            if (re.bidirectional) {
                edges.add(new Edge(syntheticEdgeId.incrementAndGet(), re.to, re.from, mode,
                        re.seconds, re.costCents, re.meters, false, re.reliability));
            }
        }
        // Transfer edges between all layer pairs at each stop (both directions).
        for (SampleCityData.Transfer t : data.transfers) {
            Map<TransportMode, Long> layers = layersByStop.get(t.stop);
            if (layers == null) {
                continue;
            }
            List<Map.Entry<TransportMode, Long>> entries = new ArrayList<>(layers.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                for (int j = 0; j < entries.size(); j++) {
                    if (i == j) {
                        continue;
                    }
                    long from = entries.get(i).getValue();
                    long to = entries.get(j).getValue();
                    // Transfer edges are modelled as WALK-mode moves within a stop, marked isTransfer.
                    // Cost 0, distance 0 (they represent changing platform/mode, not walking a route).
                    edges.add(new Edge(syntheticEdgeId.incrementAndGet(), from, to, TransportMode.WALK,
                            t.seconds, 0, 0, true, 0.99));
                }
            }
        }

        return new NetworkSnapshot(data.cityId, data.datasetVersion, nodes, edges);
    }

    private SampleCityData load() {
        try (InputStream in = new ClassPathResource("data/sample-city.json").getInputStream()) {
            return objectMapper.readValue(in, SampleCityData.class);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load sample city dataset", e);
        }
    }
}
