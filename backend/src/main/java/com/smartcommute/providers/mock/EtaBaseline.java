package com.smartcommute.providers.mock;

import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.providers.port.EtaProvider;
import com.smartcommute.providers.port.TrafficProvider;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Statistical ETA baseline (docs/08-ai-ml-design.md section 2.2).
 *
 * <p>This is the always-available, no-ML estimator: {@code eta = baseSeconds * trafficFactor}, with
 * a confidence range derived from a fixed relative spread. It is also the <b>fallback</b> used when
 * the optional ML service is unavailable, so the Core system never depends on ML (FR-27, NFR-7).</p>
 *
 * <p>It is a Spring component (not profile-restricted) because it must exist in every profile as the
 * fallback. When the ML adapter is enabled it wraps/falls back to this.</p>
 */
@Component
public class EtaBaseline implements EtaProvider {

    /** Relative spread used to derive the confidence band (±25%). */
    private static final double SPREAD = 0.25;

    private final TrafficProvider trafficProvider;

    public EtaBaseline(TrafficProvider trafficProvider) {
        this.trafficProvider = trafficProvider;
    }

    @Override
    public EtaEstimate estimate(long edgeId, TransportMode mode, int baseSeconds, LocalTime at) {
        double factor = 1.0;
        // Only road-based modes are affected by traffic; rail is largely independent.
        if (mode == TransportMode.BUS || mode == TransportMode.CAB || mode == TransportMode.CAR) {
            factor = trafficProvider.congestionFactor(edgeId, at);
        }
        int eta = (int) Math.round(baseSeconds * factor);
        int low = (int) Math.round(eta * (1 - SPREAD));
        int high = (int) Math.round(eta * (1 + SPREAD));
        return new EtaEstimate(eta, low, high, 0.6, "baseline");
    }
}
