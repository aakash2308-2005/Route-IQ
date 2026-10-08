package com.smartcommute.domain.graph;

/**
 * A vertex in the transport graph.
 *
 * <p>In the mode-expanded (layered) model, a single physical stop such as "Central" is represented
 * by several {@code Node}s that share the same {@code stopName} but differ by {@code layer}
 * (Central@WALK, Central@METRO, Central@BUS). This lets us model transfers as explicit edges and
 * filter by allowed mode cleanly. See docs/07-algorithms.md section 2.1.</p>
 *
 * @param id       unique node id (per-layer)
 * @param stopName the physical stop name (shared across layers of the same stop)
 * @param location coordinate of the stop
 * @param layer    the transport-mode layer this node belongs to
 */
public record Node(long id, String stopName, GeoPoint location, TransportMode layer) {

    public Node {
        if (stopName == null || stopName.isBlank()) {
            throw new IllegalArgumentException("stopName must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (layer == null) {
            throw new IllegalArgumentException("layer must not be null");
        }
    }
}
