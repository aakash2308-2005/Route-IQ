package com.smartcommute.persistence.entity;

import com.smartcommute.domain.constraint.RankingObjective;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** JPA entity for the {@code trip_history} table (docs/05-database-design.md section 3.4). */
@Entity
@Table(name = "trip_history")
public class TripHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_label", nullable = false, length = 120)
    private String sourceLabel;

    @Column(name = "dest_label", nullable = false, length = 120)
    private String destLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingObjective objective;

    @Column(name = "total_seconds", nullable = false)
    private int totalSeconds;

    @Column(name = "total_cost_cents", nullable = false)
    private int totalCostCents;

    @Column(name = "total_walk_meters", nullable = false)
    private int totalWalkMeters;

    @Column(name = "transfer_count", nullable = false)
    private int transferCount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected TripHistoryEntity() {
    }

    public TripHistoryEntity(Long userId, String sourceLabel, String destLabel, RankingObjective objective,
                             int totalSeconds, int totalCostCents, int totalWalkMeters, int transferCount) {
        this.userId = userId;
        this.sourceLabel = sourceLabel;
        this.destLabel = destLabel;
        this.objective = objective;
        this.totalSeconds = totalSeconds;
        this.totalCostCents = totalCostCents;
        this.totalWalkMeters = totalWalkMeters;
        this.transferCount = transferCount;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getDestLabel() {
        return destLabel;
    }

    public RankingObjective getObjective() {
        return objective;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public int getTotalCostCents() {
        return totalCostCents;
    }

    public int getTotalWalkMeters() {
        return totalWalkMeters;
    }

    public int getTransferCount() {
        return transferCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
