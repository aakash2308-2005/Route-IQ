package com.smartcommute.web.controller;

import com.smartcommute.application.history.TripHistoryService;
import com.smartcommute.persistence.entity.TripHistoryEntity;
import com.smartcommute.security.CurrentUser;
import com.smartcommute.web.dto.MeDtos;
import com.smartcommute.web.error.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trip-history endpoint (docs/06-api-design.md section 6). Paginated and scoped to the calling user.
 */
@RestController
@RequestMapping("/api/v1/me/history")
public class HistoryController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TripHistoryService historyService;
    private final CurrentUser currentUser;

    public HistoryController(TripHistoryService historyService, CurrentUser currentUser) {
        this.historyService = historyService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public ResponseEntity<MeDtos.HistoryPage> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) {
            throw new BadRequestException("page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        long userId = currentUser.requireUserId();
        Page<TripHistoryEntity> result = historyService.list(userId, page, size);
        var items = result.getContent().stream().map(this::toItem).toList();
        MeDtos.HistoryPage body = new MeDtos.HistoryPage(
                page, size, result.getTotalElements(), result.getTotalPages(), items);
        return ResponseEntity.ok(body);
    }

    private MeDtos.HistoryItem toItem(TripHistoryEntity e) {
        return new MeDtos.HistoryItem(e.getId(), e.getSourceLabel(), e.getDestLabel(), e.getObjective(),
                e.getTotalSeconds(), e.getTotalCostCents(), e.getTransferCount(), e.getCreatedAt());
    }
}
