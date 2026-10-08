package com.smartcommute.domain.graph;

/**
 * A directed, weighted, mode-labelled edge.
 *
 * <p>Two kinds of edges exist:</p>
 * <ul>
 *   <li><b>Travel edges</b> ({@code isTransfer=false}): move between two stops within one mode
 *       layer (e.g. METRO along a line). Carry travel time, cost and distance.</li>
 *   <li><b>Transfer edges</b> ({@code isTransfer=true}): connect layers at the same physical stop
 *       (e.g. METRO -> WALK -> BUS). Carry a time penalty and increment the transfer count.
 *       Cost is typically 0.</li>
 * </ul>
 *
 * <p>Money is stored as integer <b>cents</b> to avoid floating-point rounding bugs. All numeric
 * weights are non-negative — this is the precondition that lets us use Dijkstra/A* rather than
 * Bellman-Ford (see docs/07-algorithms.md sections 2.2 and 3).</p>
 *
 * @param id                unique edge id
 * @param fromNodeId        source node id
 * @param toNodeId          target node id
 * @param mode              the transport mode used to traverse this edge
 * @param baseTravelSeconds static base travel time in seconds (dynamic factors applied by WeightFunction)
 * @param costCents         monetary cost in integer cents
 * @param distanceMeters    physical distance in meters (walking distance for WALK edges)
 * @param isTransfer        true if this edge represents a transfer between layers
 * @param reliabilityScore  reliability in [0,1]; higher = more reliable
 */
public record Edge(
        long id,
        long fromNodeId,
        long toNodeId,
        TransportMode mode,
        int baseTravelSeconds,
        int costCents,
        int distanceMeters,
        boolean isTransfer,
        double reliabilityScore) {

    public Edge {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (baseTravelSeconds < 0) {
            throw new IllegalArgumentException("baseTravelSeconds must be >= 0 (Dijkstra precondition)");
        }
        if (costCents < 0) {
            throw new IllegalArgumentException("costCents must be >= 0");
        }
        if (distanceMeters < 0) {
            throw new IllegalArgumentException("distanceMeters must be >= 0");
        }
        if (reliabilityScore < 0.0 || reliabilityScore > 1.0) {
            throw new IllegalArgumentException("reliabilityScore must be in [0,1]");
        }
    }

    /** Walking distance contributed by this edge (0 for non-walk modes). */
    public int walkingMeters() {
        return mode == TransportMode.WALK ? distanceMeters : 0;
    }
}
