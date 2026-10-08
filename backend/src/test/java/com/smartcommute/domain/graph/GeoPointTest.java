package com.smartcommute.domain.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoPointTest {

    @Test
    void rejectsOutOfRangeCoordinates() {
        assertThatThrownBy(() -> new GeoPoint(91.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0.0, 200.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void haversineIsZeroForSamePoint() {
        GeoPoint p = new GeoPoint(12.97, 77.60);
        assertThat(p.haversineMeters(p)).isEqualTo(0.0);
    }

    @Test
    void haversineIsSymmetricAndPositive() {
        GeoPoint a = new GeoPoint(12.9750, 77.6050);
        GeoPoint b = new GeoPoint(12.9820, 77.5990);
        double ab = a.haversineMeters(b);
        double ba = b.haversineMeters(a);
        assertThat(ab).isGreaterThan(0.0);
        assertThat(ab).isCloseTo(ba, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void haversineApproximatesKnownDistance() {
        // ~111km per degree of latitude near the equator.
        GeoPoint a = new GeoPoint(0.0, 0.0);
        GeoPoint b = new GeoPoint(1.0, 0.0);
        assertThat(a.haversineMeters(b)).isCloseTo(111_195.0, org.assertj.core.data.Offset.offset(500.0));
    }
}
