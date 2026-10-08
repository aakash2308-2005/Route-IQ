package com.smartcommute.domain.graph;

/**
 * A mode of transport. Each mode forms a "layer" in the mode-expanded graph
 * (see docs/07-algorithms.md section 2.1). Travel edges stay within a layer;
 * transfer edges cross layers at the same physical stop and carry a penalty.
 */
public enum TransportMode {
    WALK,
    BUS,
    METRO,
    CAB,
    BIKE,
    CAR
}
