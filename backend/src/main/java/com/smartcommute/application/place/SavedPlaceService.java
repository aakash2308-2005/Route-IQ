package com.smartcommute.application.place;

import com.smartcommute.domain.graph.GeoPoint;
import com.smartcommute.domain.graph.TransportMode;
import com.smartcommute.persistence.entity.SavedPlaceEntity;
import com.smartcommute.persistence.repository.SavedPlaceRepository;
import com.smartcommute.providers.port.MapProvider;
import com.smartcommute.web.error.ConflictException;
import com.smartcommute.web.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for a user's saved places (FR-33, FR-34). All reads/writes are scoped to the calling user's
 * id, which is how ownership is enforced (NFR-14) — a user can never see or delete another user's
 * places.
 */
@Service
public class SavedPlaceService {

    private final SavedPlaceRepository repository;
    private final MapProvider mapProvider;

    public SavedPlaceService(SavedPlaceRepository repository, MapProvider mapProvider) {
        this.repository = repository;
        this.mapProvider = mapProvider;
    }

    @Transactional(readOnly = true)
    public List<SavedPlaceEntity> list(long userId) {
        return repository.findByUserId(userId);
    }

    @Transactional
    public SavedPlaceEntity create(long userId, String label, double lat, double lon) {
        if (repository.existsByUserIdAndLabel(userId, label)) {
            throw new ConflictException("a place with this label already exists");
        }
        Long nodeId = mapProvider.nearestNodeId(new GeoPoint(lat, lon), TransportMode.WALK).orElse(null);
        return repository.save(new SavedPlaceEntity(userId, label, lat, lon, nodeId));
    }

    @Transactional
    public void delete(long userId, long placeId) {
        SavedPlaceEntity place = repository.findByIdAndUserId(placeId, userId)
                .orElseThrow(() -> new NotFoundException("place not found"));
        repository.delete(place);
    }
}
