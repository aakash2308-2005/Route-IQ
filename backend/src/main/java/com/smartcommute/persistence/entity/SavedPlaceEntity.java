package com.smartcommute.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** JPA entity for the {@code saved_places} table (docs/05-database-design.md section 3.3). */
@Entity
@Table(name = "saved_places")
public class SavedPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String label;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lon;

    @Column(name = "node_id")
    private Long nodeId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SavedPlaceEntity() {
    }

    public SavedPlaceEntity(Long userId, String label, double lat, double lon, Long nodeId) {
        this.userId = userId;
        this.label = label;
        this.lat = lat;
        this.lon = lon;
        this.nodeId = nodeId;
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

    public String getLabel() {
        return label;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public Long getNodeId() {
        return nodeId;
    }
}
