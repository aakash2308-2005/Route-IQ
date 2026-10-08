package com.smartcommute.providers.mock;

import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.providers.port.EtaProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class EtaBaselineTest {

    private final EtaBaseline baseline = new EtaBaseline(new MockTrafficProvider());

    @Test
    void railModesAreUnaffectedByTraffic() {
        EtaProvider.EtaEstimate peak = baseline.estimate(1, TransportMode.METRO, 600, LocalTime.of(9, 0));
        EtaProvider.EtaEstimate offPeak = baseline.estimate(1, TransportMode.METRO, 600, LocalTime.of(3, 0));
        assertThat(peak.etaSeconds()).isEqualTo(600);
        assertThat(offPeak.etaSeconds()).isEqualTo(600);
    }

    @Test
    void roadModesSlowDownAtPeak() {
        EtaProvider.EtaEstimate peak = baseline.estimate(1, TransportMode.BUS, 600, LocalTime.of(9, 0));
        EtaProvider.EtaEstimate offPeak = baseline.estimate(1, TransportMode.BUS, 600, LocalTime.of(3, 0));
        assertThat(peak.etaSeconds()).isGreaterThan(offPeak.etaSeconds());
    }

    @Test
    void confidenceRangeBracketsEstimate() {
        EtaProvider.EtaEstimate est = baseline.estimate(1, TransportMode.BUS, 600, LocalTime.of(12, 0));
        assertThat(est.lowSeconds()).isLessThanOrEqualTo(est.etaSeconds());
        assertThat(est.highSeconds()).isGreaterThanOrEqualTo(est.etaSeconds());
        assertThat(est.source()).isEqualTo("baseline");
    }
}
