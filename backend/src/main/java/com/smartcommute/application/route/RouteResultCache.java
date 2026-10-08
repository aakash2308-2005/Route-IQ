package com.smartcommute.application.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommute.domain.graph.TransportMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * Redis-backed cache for route results (docs/02-architecture.md section 7).
 *
 * <p>Design points:</p>
 * <ul>
 *   <li><b>Normalized key:</b> departure time is bucketed to 5-minute slots and mode sets are
 *       sorted, so near-identical requests share a cache entry (higher hit rate).</li>
 *   <li><b>Short TTL:</b> results expire quickly because traffic changes (NFR-2).</li>
 *   <li><b>Fail-open:</b> any Redis error is swallowed and treated as a cache miss, so the system
 *       keeps working when Redis is down (NFR-7).</li>
 * </ul>
 */
@Component
public class RouteResultCache {

    private static final Logger log = LoggerFactory.getLogger(RouteResultCache.class);
    private static final int TIME_BUCKET_MINUTES = 5;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RouteResultCache(StringRedisTemplate redis, ObjectMapper objectMapper,
                            @Value("${smartcommute.cache.route-ttl-seconds:90}") long ttlSeconds) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Optional<List<RouteResult>> get(PlanRouteCommand cmd) {
        String key = key(cmd);
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            RouteResult[] arr = objectMapper.readValue(json, RouteResult[].class);
            return Optional.of(List.of(arr));
        } catch (Exception e) {
            log.warn("route cache read failed (fail-open): {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(PlanRouteCommand cmd, List<RouteResult> results) {
        String key = key(cmd);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(results), ttl);
        } catch (Exception e) {
            log.warn("route cache write failed (fail-open): {}", e.getMessage());
        }
    }

    /** Builds a stable, normalized cache key and hashes it to keep it short. */
    String key(PlanRouteCommand cmd) {
        StringJoiner sj = new StringJoiner("|");
        sj.add(safe(cmd.source()));
        sj.add(safe(cmd.destination()));
        sj.add(bucketTime(cmd));
        sj.add(sortedModes(cmd.allowedModes()));
        sj.add(String.valueOf(cmd.maxWalkingMeters()));
        sj.add(String.valueOf(cmd.maxCostCents()));
        sj.add(String.valueOf(cmd.maxTransfers()));
        sj.add(String.valueOf(cmd.objective()));
        sj.add(String.valueOf(cmd.k()));
        return "route:" + sha256(sj.toString());
    }

    private String bucketTime(PlanRouteCommand cmd) {
        int minutes = cmd.effectiveTime().getHour() * 60 + cmd.effectiveTime().getMinute();
        int bucket = minutes / TIME_BUCKET_MINUTES;
        return "t" + bucket;
    }

    private static String sortedModes(java.util.Set<TransportMode> modes) {
        if (modes == null || modes.isEmpty()) {
            return "ALL";
        }
        TreeSet<String> sorted = new TreeSet<>();
        for (TransportMode m : modes) {
            sorted.add(m.name());
        }
        return String.join(",", sorted);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            // SHA-256 is always available; fall back to hashCode if somehow not.
            return Integer.toHexString(input.hashCode());
        }
    }
}
