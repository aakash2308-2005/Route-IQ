package com.smartcommute.web.dto;

import com.smartcommute.application.route.RouteResult;
import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;
import java.util.Set;

/** Request/response DTOs for route planning (docs/06-api-design.md section 3). */
public final class RouteDtos {

    private RouteDtos() {
    }

    public record PlanRequest(
            @NotBlank String source,
            @NotBlank String destination,
            // At least one of departAt/arrivalBefore should be given; validated in the controller.
            String departAt,          // "HH:mm"
            String arrivalBefore,     // "HH:mm"
            Set<TransportMode> allowedModes,
            @PositiveOrZero Integer maxWalkingMeters,
            @PositiveOrZero Integer maxCostCents,
            @PositiveOrZero Integer maxTransfers,
            RankingObjective objective,
            @Min(1) @Max(5) Integer k) {
    }

    public record PlanResponse(
            String requestId,
            RankingObjective objective,
            int count,
            List<RouteResult> routes,
            String note) {
    }
}
