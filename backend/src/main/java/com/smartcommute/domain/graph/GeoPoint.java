package com.smartcommute.domain.graph;

/**
 * An immutable geographic coordinate (WGS84 degrees).
 *
 * <p>Used for map/geocoding resolution and as the basis for the A* heuristic
 * (haversine distance, see docs/07-algorithms.md section 3.5).</p>
 */
public record GeoPoint(double lat, double lon) {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public GeoPoint {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("latitude out of range: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("longitude out of range: " + lon);
        }
    }

    /**
     * Great-circle (haversine) distance in meters between this point and another.
     *
     * <p>This is an <em>admissible</em> lower bound on real travel distance (a straight line is
     * never longer than any actual path), which is exactly what makes it a valid A* heuristic.</p>
     */
    public double haversineMeters(GeoPoint other) {
        double lat1 = Math.toRadians(this.lat);
        double lat2 = Math.toRadians(other.lat);
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLon = Math.toRadians(other.lon - this.lon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
