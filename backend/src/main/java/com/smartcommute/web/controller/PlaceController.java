package com.smartcommute.web.controller;

import com.smartcommute.application.place.SavedPlaceService;
import com.smartcommute.persistence.entity.SavedPlaceEntity;
import com.smartcommute.security.CurrentUser;
import com.smartcommute.web.dto.MeDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Saved-place endpoints (docs/06-api-design.md section 5). Ownership is enforced by scoping every
 * query to the JWT user id (NFR-14).
 */
@RestController
@RequestMapping("/api/v1/me/places")
public class PlaceController {

    private final SavedPlaceService placeService;
    private final CurrentUser currentUser;

    public PlaceController(SavedPlaceService placeService, CurrentUser currentUser) {
        this.placeService = placeService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<List<MeDtos.PlaceResponse>> list() {
        long userId = currentUser.requireUserId();
        List<MeDtos.PlaceResponse> body = placeService.list(userId).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<MeDtos.PlaceResponse> create(@Valid @RequestBody MeDtos.CreatePlaceRequest req) {
        long userId = currentUser.requireUserId();
        SavedPlaceEntity created = placeService.create(userId, req.label(), req.lat(), req.lon());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        long userId = currentUser.requireUserId();
        placeService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private MeDtos.PlaceResponse toResponse(SavedPlaceEntity e) {
        return new MeDtos.PlaceResponse(e.getId(), e.getLabel(), e.getLat(), e.getLon(), e.getNodeId());
    }
}
