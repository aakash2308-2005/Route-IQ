package com.smartcommute.web.dto;

import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/** DTOs for preferences, saved places and history (docs/06-api-design.md sections 4-6). */
public final class MeDtos {

    private MeDtos() {
    }

    // --- Preferences ---
    public record PreferencesBody(
            RankingObjective objective,
            @PositiveOrZero Integer maxWalkingMeters,
            @PositiveOrZero Integer maxCostCents,
            @PositiveOrZero Integer maxTransfers,
            Set<TransportMode> preferredModes,
            Set<TransportMode> avoidedModes) {
    }

    // --- Saved places ---
    public record CreatePlaceRequest(
            @NotBlank @Size(max = 80) String label,
            double lat,
            double lon) {
    }

    public record PlaceResponse(long id, String label, double lat, double lon, Long nodeId) {
    }

    // --- History ---
    public record HistoryItem(
            long id, String sourceLabel, String destLabel, RankingObjective objective,
            int totalSeconds, int totalCostCents, int transferCount, OffsetDateTime createdAt) {
    }

    public record HistoryPage(
            int page, int size, long totalElements, int totalPages, List<HistoryItem> items) {
    }
}
