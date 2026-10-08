package com.smartcommute.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end flow (docs/09 T12.2): register -> login -> plan a route -> read history.
 * Exercises auth, JWT, the route engine, ranking, caching and persistence together.
 */
class RoutePlanningFlowIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void registerLoginPlanAndHistory() {
        // 1. Register
        String email = "flow-" + System.nanoTime() + "@example.com";
        ResponseEntity<JsonNode> register = rest.postForEntity("/api/v1/auth/register",
                json("{\"email\":\"" + email + "\",\"password\":\"S3curePass!\",\"displayName\":\"Flow\"}"),
                JsonNode.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Login
        ResponseEntity<JsonNode> login = rest.postForEntity("/api/v1/auth/login",
                json("{\"email\":\"" + email + "\",\"password\":\"S3curePass!\"}"), JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().get("token").asText();
        assertThat(token).isNotBlank();

        // 3. Plan a route (Central -> TechPark)
        HttpHeaders auth = new HttpHeaders();
        auth.setContentType(MediaType.APPLICATION_JSON);
        auth.setBearerAuth(token);
        String planBody = "{\"source\":\"Central\",\"destination\":\"TechPark\",\"departAt\":\"08:30\","
                + "\"objective\":\"FASTEST\",\"k\":3}";
        ResponseEntity<JsonNode> plan = rest.exchange("/api/v1/routes/plan", HttpMethod.POST,
                new HttpEntity<>(planBody, auth), JsonNode.class);
        assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(plan.getBody().get("count").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(plan.getBody().get("routes").get(0).get("legs")).isNotEmpty();

        // 4. Plan again -> should be a cache HIT
        ResponseEntity<JsonNode> plan2 = rest.exchange("/api/v1/routes/plan", HttpMethod.POST,
                new HttpEntity<>(planBody, auth), JsonNode.class);
        assertThat(plan2.getHeaders().getFirst("X-Cache")).isEqualTo("HIT");

        // 5. History reflects the search (async; retry briefly)
        JsonNode history = awaitHistory(auth);
        assertThat(history.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        String planBody = "{\"source\":\"Central\",\"destination\":\"TechPark\",\"departAt\":\"08:30\"}";
        ResponseEntity<JsonNode> plan = rest.postForEntity("/api/v1/routes/plan", json(planBody), JsonNode.class);
        assertThat(plan.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private JsonNode awaitHistory(HttpHeaders auth) {
        for (int attempt = 0; attempt < 20; attempt++) {
            ResponseEntity<JsonNode> history = rest.exchange("/api/v1/me/history?page=0&size=20",
                    HttpMethod.GET, new HttpEntity<>(auth), JsonNode.class);
            if (history.getStatusCode() == HttpStatus.OK && history.getBody().get("totalElements").asInt() >= 1) {
                return history.getBody();
            }
            sleep();
        }
        throw new AssertionError("history was not recorded in time");
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
