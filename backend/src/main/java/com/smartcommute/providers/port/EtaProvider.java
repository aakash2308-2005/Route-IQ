package com.smartcommute.providers.port;

import com.smartcommute.domain.graph.TransportMode;

import java.time.LocalTime;

/**
 * Port for travel-time estimation (docs/08-ai-ml-design.md).
 *
 * <p>Two implementations exist behind this port: a statistical {@code EtaBaseline} (always present,
 * no ML) and an optional {@code EtaMlAdapter} that calls the Python ML service. The ML adapter is
 * wrapped with timeout + circuit breaker and falls back to the baseline, so the Core system never
 * depends on ML being up (FR-27, NFR-7).</p>
 */
public interface EtaProvider {

    /** An ETA estimate in seconds with a confidence range. */
    record EtaEstimate(int etaSeconds, int lowSeconds, int highSeconds, double confidence, String source) {
    }

    /** Estimate travel seconds for an edge given contextual features. */
    EtaEstimate estimate(long edgeId, TransportMode mode, int baseSeconds, LocalTime at);
}
