package com.loopy.dataport.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @RequestParam, @Valid, @RequestBody, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.dataport.dto.ExportResponse;
import com.loopy.dataport.dto.ImportRequest;
import com.loopy.dataport.dto.ImportResponse;
import com.loopy.dataport.service.DataPortService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dataport")
public class DataPortController {

    private final DataPortService dataPortService;

    public DataPortController(DataPortService dataPortService) {
        this.dataPortService = dataPortService;
    }

    /**
     * Export user's topics/concepts/cards as JSON.
     * Optionally filter by topic IDs.
     */
    @GetMapping("/export")
    public ResponseEntity<ExportResponse> exportData(
            @RequestParam(required = false) List<UUID> topicIds,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dataPortService.exportData(user, topicIds));
    }

    /**
     * Import topics/concepts/cards from a previously exported JSON payload.
     * All entities are created fresh for the authenticated user.
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importData(
            @Valid @RequestBody ImportRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dataPortService.importData(request, user));
    }
}
