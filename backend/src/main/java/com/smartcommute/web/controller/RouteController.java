package com.smartcommute.web.controller;

import com.smartcommute.application.route.PlanRouteCommand;
import com.smartcommute.application.route.RoutePlanningService;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.observability.CorrelationIdFilter;
import com.smartcommute.observability.RateLimiter;
import com.smartcommute.security.CurrentUser;
import com.smartcommute.web.dto.RouteDtos;
import com.smartcommute.web.error.BadRequestException;
import com.smartcommute.web.error.RateLimitedException;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Route planning endpoint (docs/06-api-design.md section 3).
 *
 * <p>Translates the HTTP DTO into a domain {@link PlanRouteCommand}, applies per-user rate limiting,
 * delegates to {@link RoutePlanningService}, and sets the {@code X-Cache} header. An empty result is
 * a valid 200 response with an explanatory note (never an error) — this is how "no route satisfies
 * the constraints" is surfaced (FR-21).</p>
 */
@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private static final int DEFAULT_K = 3;

    private final RoutePlanningService planningService;
    private final CurrentUser currentUser;
    private final RateLimiter rateLimiter;

    public RouteController(RoutePlanningService planningService, CurrentUser currentUser, RateLimiter rateLimiter) {
        this.planningService = planningService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/plan")
    public ResponseEntity<RouteDtos.PlanResponse> plan(@Valid @RequestBody RouteDtos.PlanRequest req) {
        long userId = currentUser.requireUserId();

        if (!rateLimiter.allow("plan:" + userId, 60, Duration.ofSeconds(60))) {
            throw new RateLimitedException("too many planning requests; try again shortly");
        }

        LocalTime departAt = parseTime(req.departAt(), "departAt");
        LocalTime arrivalBefore = parseTime(req.arrivalBefore(), "arrivalBefore");
        if (departAt == null && arrivalBefore == null) {
            throw new BadRequestException("at least one of departAt or arrivalBefore is required");
        }

        RankingObjective objective = req.objective() != null ? req.objective() : RankingObjective.BALANCED;
        int k = req.k() != null ? req.k() : DEFAULT_K;

        PlanRouteCommand cmd = new PlanRouteCommand(
                userId, req.source(), req.destination(), departAt, arrivalBefore,
                req.allowedModes(), req.maxWalkingMeters(), req.maxCostCents(), req.maxTransfers(),
                objective, k);

        RoutePlanningService.PlanOutcome outcome = planningService.plan(cmd);

        RouteDtos.PlanResponse body = new RouteDtos.PlanResponse(
                MDC.get(CorrelationIdFilter.MDC_KEY),
                objective,
                outcome.routes().size(),
                outcome.routes(),
                outcome.note());

        return ResponseEntity.ok()
                .header("X-Cache", outcome.fromCache() ? "HIT" : "MISS")
                .body(body);
    }

    private LocalTime parseTime(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(field + " must be a valid time in HH:mm format");
        }
    }
}
