package com.loopy.card.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @Valid, @RequestBody, @PathVariable, @RequestParam, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.card.service.ConceptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/concepts")
public class ConceptController {

    private final ConceptService conceptService;

    public ConceptController(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    @GetMapping
    public ResponseEntity<List<ConceptResponse>> list(@RequestParam UUID topicId,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(conceptService.getConcepts(topicId, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConceptResponse> get(@PathVariable UUID id,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(conceptService.getConcept(id, user));
    }

    @PostMapping
    public ResponseEntity<ConceptResponse> create(@Valid @RequestBody CreateConceptRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(conceptService.createConcept(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConceptResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody UpdateConceptRequest request,
                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(conceptService.updateConcept(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal User user) {
        conceptService.deleteConcept(id, user);
        return ResponseEntity.noContent().build();
    }
}
