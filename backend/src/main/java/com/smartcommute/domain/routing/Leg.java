package com.smartcommute.domain.routing;

import com.smartcommute.domain.graph.TransportMode;

/**
 * One segment of a journey: traversal of a single edge. A sequence of legs makes a {@link RoutePath}.
 *
 * @param mode           transport mode for this leg
 * @param fromNodeId     origin node id
 * @param toNodeId       destination node id
 * @param travelSeconds  effective travel time (after dynamic factors were applied during search)
 * @param costCents      cost in integer cents
 * @param distanceMeters distance in meters
 * @param isTransfer     true if this leg is a transfer between mode layers
 */
public record Leg(
        TransportMode mode,
        long fromNodeId,
        long toNodeId,
        int travelSeconds,
        int costCents,
        int distanceMeters,
        boolean isTransfer) {
}
