package com.smartcommute.providers.mock;

import com.smartcommute.application.route.GraphProvider;
import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.Node;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.providers.port.MapProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Mock map/geocoding provider (default {@code mock} profile).
 *
 * <p>Resolves a place string by matching against known stop names in the loaded network (case- and
 * whitespace-insensitive). Nearest-node resolution does a linear scan over nodes using haversine
 * distance — perfectly adequate for the small sample network. A real adapter would call a geocoding
 * API and use a spatial index.</p>
 */
@Component
@Profile("mock")
public class MockMapProvider implements MapProvider {

    private final GraphProvider graphProvider;

    public MockMapProvider(GraphProvider graphProvider) {
        this.graphProvider = graphProvider;
    }

    @Override
    public Optional<GeoPoint> geocode(String query) {
        String normalized = normalize(query);
        return graphProvider.snapshot().nodes().stream()
                .filter(n -> normalize(n.stopName()).equals(normalized))
                .map(Node::location)
                .findFirst();
    }

    @Override
    public Optional<Long> nearestNodeId(GeoPoint point, TransportMode preferredLayer) {
        Node best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Node n : graphProvider.snapshot().nodes()) {
            if (preferredLayer != null && n.layer() != preferredLayer) {
                continue;
            }
            double d = n.location().haversineMeters(point);
            if (d < bestDistance) {
                bestDistance = d;
                best = n;
            }
        }
        return Optional.ofNullable(best).map(Node::id);
    }

    @Override
    public Optional<Long> resolveToNode(String query) {
        // Prefer an exact stop-name match on the WALK layer (natural journey start/end).
        String normalized = normalize(query);
        Optional<Long> walkNode = graphProvider.snapshot().nodes().stream()
                .filter(n -> n.layer() == TransportMode.WALK && normalize(n.stopName()).equals(normalized))
                .map(Node::id)
                .findFirst();
        if (walkNode.isPresent()) {
            return walkNode;
        }
        // Fall back to geocode + nearest WALK node.
        return geocode(query).flatMap(p -> nearestNodeId(p, TransportMode.WALK));
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
