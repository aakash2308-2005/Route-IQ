package com.smartcommute.providers.mock;

import java.util.List;
import java.util.Map;

/**
 * Java view of {@code resources/data/sample-city.json}, populated by Jackson. Kept intentionally
 * simple (loose types) since it is just the on-disk representation that the mock transit provider
 * translates into domain {@code Node}/{@code Edge} objects.
 */
public final class SampleCityData {

    public String cityId;
    public String datasetVersion;
    public List<Stop> stops;
    public List<RawNode> nodes;
    public List<Transfer> transfers;
    public List<RawEdge> edges;

    /** Map of stop name -> coordinate, filled after load for quick lookup. */
    public Map<String, Stop> stopsByName;

    public static final class Stop {
        public String name;
        public double lat;
        public double lon;
    }

    public static final class RawNode {
        public long id;
        public String stop;
        public String layer;
    }

    public static final class Transfer {
        public String stop;
        public int seconds;
    }

    public static final class RawEdge {
        public long id;
        public long from;
        public long to;
        public String mode;
        public int seconds;
        public int costCents;
        public int meters;
        public double reliability;
        public boolean bidirectional;
    }
}
