package com.loopy.teachback.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @Valid, @RequestBody, @RequestParam, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.teachback.dto.TeachBackResponse;
import com.loopy.teachback.service.TeachBackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teach-back")
public class TeachBackController {

    private final TeachBackService teachBackService;

    public TeachBackController(TeachBackService teachBackService) {
        this.teachBackService = teachBackService;
    }

    /** Returns concepts that require teach-back. */
    @GetMapping("/pending")
    public ResponseEntity<List<ConceptResponse>> pending(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teachBackService.getConceptsRequiringTeachBack(user));
    }

    /** Submits a teach-back session. */
    @PostMapping
    public ResponseEntity<TeachBackResponse> submit(@Valid @RequestBody SubmitTeachBackRequest request,
                                                    @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teachBackService.submitTeachBack(request, user));
    }

    /** Returns teach-back history for a concept. */
    @GetMapping("/history")
    public ResponseEntity<List<TeachBackResponse>> history(@RequestParam UUID conceptId,
                                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teachBackService.getHistory(conceptId, user));
    }
}
