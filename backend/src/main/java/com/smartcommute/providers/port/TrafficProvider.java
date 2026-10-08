package com.smartcommute.providers.port;

import java.time.LocalTime;

/**
 * Port for dynamic traffic factors (docs/04-lld.md section 4).
 *
 * <p>Returns a congestion multiplier applied to road/cab travel times: {@code 1.0} means free flow,
 * {@code 1.5} means 50% slower. The mock adapter models rush-hour peaks from the sample dataset; a
 * real adapter would call a traffic API. Falls back to {@code 1.0} on failure (graceful
 * degradation, NFR-7).</p>
 */
public interface TrafficProvider {

    /** Congestion multiplier (>= 1.0) for an edge at a given time of day. */
    double congestionFactor(long edgeId, LocalTime at);
}
