package com.smartcommute.persistence.entity;

import com.smartcommute.domain.constraint.RankingObjective;
import com.smartcommute.domain.graph.TransportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.EnumSet;
import java.util.Set;

/** JPA entity for the {@code preferences} table (docs/05-database-design.md section 3.2). */
@Entity
@Table(name = "preferences")
public class PreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingObjective objective = RankingObjective.BALANCED;

    @Column(name = "max_walking_meters")
    private Integer maxWalkingMeters;

    @Column(name = "max_cost_cents")
    private Integer maxCostCents;

    @Column(name = "max_transfers")
    private Integer maxTransfers;

    @Convert(converter = TransportModeSetConverter.class)
    @Column(name = "preferred_modes", nullable = false, columnDefinition = "text")
    private Set<TransportMode> preferredModes = EnumSet.noneOf(TransportMode.class);

    @Convert(converter = TransportModeSetConverter.class)
    @Column(name = "avoided_modes", nullable = false, columnDefinition = "text")
    private Set<TransportMode> avoidedModes = EnumSet.noneOf(TransportMode.class);

    protected PreferenceEntity() {
    }

    public PreferenceEntity(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public RankingObjective getObjective() {
        return objective;
    }

    public void setObjective(RankingObjective objective) {
        this.objective = objective;
    }

    public Integer getMaxWalkingMeters() {
        return maxWalkingMeters;
    }

    public void setMaxWalkingMeters(Integer maxWalkingMeters) {
        this.maxWalkingMeters = maxWalkingMeters;
    }

    public Integer getMaxCostCents() {
        return maxCostCents;
    }

    public void setMaxCostCents(Integer maxCostCents) {
        this.maxCostCents = maxCostCents;
    }

    public Integer getMaxTransfers() {
        return maxTransfers;
    }

    public void setMaxTransfers(Integer maxTransfers) {
        this.maxTransfers = maxTransfers;
    }

    public Set<TransportMode> getPreferredModes() {
        return preferredModes;
    }

    public void setPreferredModes(Set<TransportMode> preferredModes) {
        this.preferredModes = preferredModes;
    }

    public Set<TransportMode> getAvoidedModes() {
        return avoidedModes;
    }

    public void setAvoidedModes(Set<TransportMode> avoidedModes) {
        this.avoidedModes = avoidedModes;
    }
}
