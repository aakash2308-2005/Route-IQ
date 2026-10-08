package com.smartcommute.providers.real;

import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.providers.mock.EtaBaseline;
import com.smartcommute.providers.port.EtaProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;

/**
 * Optional ML-backed ETA provider (docs/08-ai-ml-design.md section 2.6).
 *
 * <p>Calls the Python ML service over HTTP. It is wrapped with a Resilience4j circuit breaker and
 * time limiter: on timeout, error, or open circuit it <b>falls back to the statistical
 * {@link EtaBaseline}</b>, so the Core system never depends on ML being available (FR-27, NFR-7,
 * NFR-8).</p>
 *
 * <p>Enabled only when {@code smartcommute.eta.ml.enabled=true}; otherwise the baseline is the sole
 * {@link EtaProvider}. It is marked {@link Primary} so it takes precedence when present.</p>
 */
@Component
@Primary
@ConditionalOnProperty(prefix = "smartcommute.eta.ml", name = "enabled", havingValue = "true")
public class EtaMlAdapter implements EtaProvider {

    private static final Logger log = LoggerFactory.getLogger(EtaMlAdapter.class);
    private static final String CB_NAME = "etaMl";

    private final RestClient restClient;
    private final EtaBaseline baseline;

    public EtaMlAdapter(EtaBaseline baseline,
                        @Value("${smartcommute.eta.ml.base-url:http://localhost:8000}") String baseUrl,
                        @Value("${smartcommute.eta.ml.timeout-ms:800}") int timeoutMs) {
        this.baseline = baseline;
        // Client-level connect/read timeouts bound how long a slow ML service can block a request.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "fallback")
    public EtaEstimate estimate(long edgeId, TransportMode mode, int baseSeconds, LocalTime at) {
        MlResponse response = restClient.post()
                .uri("/predict")
                .body(Map.of(
                        "segmentId", edgeId,
                        "mode", mode.name(),
                        "hour", at != null ? at.getHour() : 12,
                        "baseSeconds", baseSeconds))
                .retrieve()
                .body(MlResponse.class);
        if (response == null) {
            return baseline.estimate(edgeId, mode, baseSeconds, at);
        }
        return new EtaEstimate(response.etaSeconds, response.low, response.high, response.confidence, "ml");
    }

    /** Fallback used by the circuit breaker on any failure/timeout/open circuit. */
    @SuppressWarnings("unused")
    private EtaEstimate fallback(long edgeId, TransportMode mode, int baseSeconds, LocalTime at, Throwable t) {
        log.warn("ETA ML call failed, using baseline: {}", t.getMessage());
        return baseline.estimate(edgeId, mode, baseSeconds, at);
    }

    /** Shape of the ML service response (docs/08 section 2.3). */
    static final class MlResponse {
        public int etaSeconds;
        public int low;
        public int high;
        public double confidence;
        public String modelVersion;
    }
}
