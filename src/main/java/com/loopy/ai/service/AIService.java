package com.loopy.ai.service;

// Dependencies: @Service, @Value — see DEPENDENCY_GUIDE.md
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopy.ai.dto.GeneratedCard;
import com.loopy.ai.dto.TeachBackEvaluation;
import com.loopy.auth.entity.User;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AIService {

    private final AnthropicClient anthropicClient;
    private final ConceptRepository conceptRepository;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.generation-model:claude-haiku-4-5-20251001}")
    private String generationModel;

    @Value("${anthropic.evaluation-model:claude-sonnet-4-5-20250514}")
    private String evaluationModel;

    public AIService(AnthropicClient anthropicClient,
                     ConceptRepository conceptRepository,
                     ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.conceptRepository = conceptRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates flashcards from provided content using Claude.
     */
    public List<GeneratedCard> generateCards(UUID conceptId, String content, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(conceptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        String system = """
                You are a flashcard generation assistant. Generate concise, effective flashcards from the provided content.
                Each card should test a single concept. Use active recall principles.

                Card types: STANDARD (question/answer), CODE_OUTPUT (what does this code output?), \
                SPOT_THE_BUG (find the error), FILL_BLANK (complete the ___), EXPLAIN_WHEN (when would you use X?), \
                COMPARE (how does X differ from Y?)

                Return ONLY a JSON array of objects with these fields:
                - "front": the question
                - "back": the answer
                - "cardType": one of the card types above
                - "hint": optional hint (or null)

                Topic: %s
                Concept: %s
                """.formatted(concept.getTopic().getName(), concept.getTitle());

        String response = anthropicClient.sendMessage(generationModel, system, content, 4096);

        return parseCards(response);
    }

    /**
     * Evaluates a teach-back explanation using Claude.
     */
    public TeachBackEvaluation evaluateTeachBack(UUID conceptId, String userExplanation, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(conceptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        String referenceInfo = concept.getReferenceExplanation() != null
                ? "Reference explanation: " + concept.getReferenceExplanation()
                : "No reference explanation available.";

        String system = """
                You are an expert learning evaluator. Evaluate the user's explanation of a concept.

                Score each dimension 1-10:
                - clarity: How clear and well-structured is the explanation?
                - accuracy: Are the facts correct?
                - completeness: Does it cover all important aspects?

                Also provide:
                - feedback: Brief constructive feedback (2-3 sentences)
                - followUpQuestions: 2-3 probing questions to deepen understanding
                - detectedGaps: Specific sub-concepts or details that were missed
                - suggestedCards: 1-3 flashcards to fill the detected gaps (same format as card generation)

                Return ONLY valid JSON with these exact fields:
                {"clarityScore": N, "accuracyScore": N, "completenessScore": N, "feedback": "...", \
                "followUpQuestions": [...], "detectedGaps": [...], \
                "suggestedCards": [{"front": "...", "back": "...", "cardType": "STANDARD", "hint": null}]}

                Concept: %s
                %s
                """.formatted(concept.getTitle(), referenceInfo);

        String response = anthropicClient.sendMessage(evaluationModel, system, userExplanation, 4096);

        return parseEvaluation(response);
    }

    /** Returns whether the AI service is available. */
    public boolean isAvailable() {
        return anthropicClient.isConfigured();
    }

    private List<GeneratedCard> parseCards(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI-generated cards: " + e.getMessage(), e);
        }
    }

    private TeachBackEvaluation parseEvaluation(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, TeachBackEvaluation.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI evaluation: " + e.getMessage(), e);
        }
    }

    /** Extracts JSON from a response that might have markdown code fences. */
    private String extractJson(String text) {
        text = text.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        return text.trim();
    }
}
