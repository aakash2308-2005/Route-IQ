package com.smartcommute.providers.port;

/**
 * Port for loading a city's static transport network (docs/04-lld.md section 4).
 *
 * <p>Implementations: {@code MockTransitProvider} (bundled synthetic dataset, default profile) and,
 * in future, a real GTFS adapter. The route engine consumes only {@link NetworkSnapshot}, so the
 * data source is fully swappable (Dependency Inversion).</p>
 */
public interface TransitProvider {

    /** Load the network for a city. Throws if the city is unknown. */
    NetworkSnapshot loadNetwork(String cityId);
}
