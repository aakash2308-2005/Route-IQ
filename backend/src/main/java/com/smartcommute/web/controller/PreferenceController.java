package com.smartcommute.web.controller;

import com.smartcommute.application.user.PreferenceService;
import com.smartcommute.persistence.entity.PreferenceEntity;
import com.smartcommute.security.CurrentUser;
import com.smartcommute.web.dto.MeDtos;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Preference endpoints (docs/06-api-design.md section 4). All operations act on the calling user's
 * own preferences — the user id comes from the JWT, never from the request body (NFR-14).
 */
@RestController
@RequestMapping("/api/v1/me/preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;
    private final CurrentUser currentUser;

    public PreferenceController(PreferenceService preferenceService, CurrentUser currentUser) {
        this.preferenceService = preferenceService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<MeDtos.PreferencesBody> get() {
        long userId = currentUser.requireUserId();
        return ResponseEntity.ok(toBody(preferenceService.getOrDefaults(userId)));
    }

    @PutMapping
    public ResponseEntity<MeDtos.PreferencesBody> update(@Valid @RequestBody MeDtos.PreferencesBody body) {
        long userId = currentUser.requireUserId();
        PreferenceEntity updated = preferenceService.update(userId, body.objective(),
                body.maxWalkingMeters(), body.maxCostCents(), body.maxTransfers(),
                body.preferredModes(), body.avoidedModes());
        return ResponseEntity.ok(toBody(updated));
    }

    private MeDtos.PreferencesBody toBody(PreferenceEntity e) {
        return new MeDtos.PreferencesBody(e.getObjective(), e.getMaxWalkingMeters(), e.getMaxCostCents(),
                e.getMaxTransfers(), e.getPreferredModes(), e.getAvoidedModes());
    }
}
