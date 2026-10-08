package com.smartcommute.providers.port;

import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.TransportMode;

import java.util.Optional;

/**
 * Port for geocoding and nearest-node resolution (docs/04-lld.md section 4).
 *
 * <p>Turns a user-supplied place string (or coordinate) into a graph node the engine can route
 * from/to. The mock adapter resolves against the bundled dataset's known stop names; a real adapter
 * would call a geocoding API.</p>
 */
public interface MapProvider {

    /** Resolve a free-text place to a coordinate, if known. */
    Optional<GeoPoint> geocode(String query);

    /**
     * Find the id of the nearest graph node to a coordinate on the given mode layer.
     * Returns empty if no node is reachable/near enough.
     */
    Optional<Long> nearestNodeId(GeoPoint point, TransportMode preferredLayer);

    /**
     * Convenience: resolve a place string directly to a node id (geocode + nearest node), preferring
     * the WALK layer as the natural start/end of a journey.
     */
    Optional<Long> resolveToNode(String query);
}
