package com.loopy.ai.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @Valid, @RequestBody, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.ai.dto.EvaluateTeachBackRequest;
import com.loopy.ai.dto.GenerateCardsRequest;
import com.loopy.ai.dto.GeneratedCard;
import com.loopy.ai.dto.TeachBackEvaluation;
import com.loopy.ai.service.AIService;
import com.loopy.auth.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    /** Check if AI features are available (API key configured). */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status() {
        return ResponseEntity.ok(Map.of("available", aiService.isAvailable()));
    }

    /** Generate flashcards from content using AI. */
    @PostMapping("/generate-cards")
    public ResponseEntity<List<GeneratedCard>> generateCards(
            @Valid @RequestBody GenerateCardsRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.generateCards(request.conceptId(), request.content(), user));
    }

    /** Evaluate a teach-back explanation using AI. */
    @PostMapping("/evaluate-teach-back")
    public ResponseEntity<TeachBackEvaluation> evaluateTeachBack(
            @Valid @RequestBody EvaluateTeachBackRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(aiService.evaluateTeachBack(request.conceptId(), request.userExplanation(), user));
    }
}
