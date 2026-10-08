package com.smartcommute.providers.mock;

import com.smartcommute.providers.port.TrafficProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Mock traffic provider (default {@code mock} profile).
 *
 * <p>Models congestion as a deterministic function of time of day: heavier during morning (08:00–
 * 10:00) and evening (17:00–20:00) peaks, free-flowing otherwise. Deterministic so tests are
 * reproducible. A real adapter would call a traffic API and fall back to this on failure.</p>
 */
@Component
@Profile("mock")
public class MockTrafficProvider implements TrafficProvider {

    @Override
    public double congestionFactor(long edgeId, LocalTime at) {
        if (at == null) {
            return 1.0;
        }
        int hour = at.getHour();
        boolean morningPeak = hour >= 8 && hour < 10;
        boolean eveningPeak = hour >= 17 && hour < 20;
        if (morningPeak || eveningPeak) {
            return 1.5;   // 50% slower at peak
        }
        if (hour >= 10 && hour < 17) {
            return 1.15;  // mild daytime congestion
        }
        return 1.0;       // free flow overnight/early
    }
}
