package com.loopy.config;

// Dependencies: Validator, ValidatorFactory, Validation — see DEPENDENCY_GUIDE.md
import com.loopy.ai.dto.EvaluateTeachBackRequest;
import com.loopy.ai.dto.GenerateCardsRequest;
import com.loopy.auth.dto.LoginRequest;
import com.loopy.auth.dto.RefreshRequest;
import com.loopy.auth.dto.RegisterRequest;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateCardRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.card.entity.CardType;
import com.loopy.dataport.dto.*;
import com.loopy.review.dto.SubmitReviewRequest;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.UpdateTopicRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for all request DTOs.
 * Uses a Validator directly to test constraint annotations.
 */
class ValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // --- RegisterRequest ---

    @Nested
    class RegisterRequestValidation {

        @Test
        void valid_noViolations() {
            RegisterRequest request = new RegisterRequest("user@example.com", "password123");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankEmail_hasViolation() {
            RegisterRequest request = new RegisterRequest("", "password123");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void invalidEmail_hasViolation() {
            RegisterRequest request = new RegisterRequest("not-an-email", "password123");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullEmail_hasViolation() {
            RegisterRequest request = new RegisterRequest(null, "password123");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankPassword_hasViolation() {
            RegisterRequest request = new RegisterRequest("user@example.com", "");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void shortPassword_hasViolation() {
            RegisterRequest request = new RegisterRequest("user@example.com", "short");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
        }

        @Test
        void passwordExactlyMinLength_noViolation() {
            RegisterRequest request = new RegisterRequest("user@example.com", "12345678");
            Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }
    }

    // --- LoginRequest ---

    @Nested
    class LoginRequestValidation {

        @Test
        void valid_noViolations() {
            LoginRequest request = new LoginRequest("user@example.com", "password");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankEmail_hasViolation() {
            LoginRequest request = new LoginRequest("", "password");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void invalidEmail_hasViolation() {
            LoginRequest request = new LoginRequest("invalid", "password");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankPassword_hasViolation() {
            LoginRequest request = new LoginRequest("user@example.com", "");
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- RefreshRequest ---

    @Nested
    class RefreshRequestValidation {

        @Test
        void valid_noViolations() {
            RefreshRequest request = new RefreshRequest("some-token");
            Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankToken_hasViolation() {
            RefreshRequest request = new RefreshRequest("");
            Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullToken_hasViolation() {
            RefreshRequest request = new RefreshRequest(null);
            Set<ConstraintViolation<RefreshRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- CreateTopicRequest ---

    @Nested
    class CreateTopicRequestValidation {

        @Test
        void valid_noViolations() {
            CreateTopicRequest request = new CreateTopicRequest("Java", "desc", "#FF0000");
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankName_hasViolation() {
            CreateTopicRequest request = new CreateTopicRequest("", "desc", null);
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nameTooLong_hasViolation() {
            String longName = "x".repeat(101);
            CreateTopicRequest request = new CreateTopicRequest(longName, null, null);
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void descriptionTooLong_hasViolation() {
            String longDesc = "x".repeat(501);
            CreateTopicRequest request = new CreateTopicRequest("Valid", longDesc, null);
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void colorHexTooLong_hasViolation() {
            CreateTopicRequest request = new CreateTopicRequest("Valid", null, "#FF000000");
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullDescription_noViolation() {
            CreateTopicRequest request = new CreateTopicRequest("Valid", null, null);
            Set<ConstraintViolation<CreateTopicRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }
    }

    // --- UpdateTopicRequest ---

    @Nested
    class UpdateTopicRequestValidation {

        @Test
        void valid_noViolations() {
            UpdateTopicRequest request = new UpdateTopicRequest("Updated", "desc", "#00FF00");
            Set<ConstraintViolation<UpdateTopicRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankName_hasViolation() {
            UpdateTopicRequest request = new UpdateTopicRequest("", null, null);
            Set<ConstraintViolation<UpdateTopicRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- CreateConceptRequest ---

    @Nested
    class CreateConceptRequestValidation {

        @Test
        void valid_noViolations() {
            CreateConceptRequest request = new CreateConceptRequest(UUID.randomUUID(), "OOP", "notes");
            Set<ConstraintViolation<CreateConceptRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullTopicId_hasViolation() {
            CreateConceptRequest request = new CreateConceptRequest(null, "OOP", null);
            Set<ConstraintViolation<CreateConceptRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankTitle_hasViolation() {
            CreateConceptRequest request = new CreateConceptRequest(UUID.randomUUID(), "", null);
            Set<ConstraintViolation<CreateConceptRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void titleTooLong_hasViolation() {
            String longTitle = "x".repeat(201);
            CreateConceptRequest request = new CreateConceptRequest(UUID.randomUUID(), longTitle, null);
            Set<ConstraintViolation<CreateConceptRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- UpdateConceptRequest ---

    @Nested
    class UpdateConceptRequestValidation {

        @Test
        void valid_noViolations() {
            UpdateConceptRequest request = new UpdateConceptRequest("Updated", "notes", null);
            Set<ConstraintViolation<UpdateConceptRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankTitle_hasViolation() {
            UpdateConceptRequest request = new UpdateConceptRequest("", null, null);
            Set<ConstraintViolation<UpdateConceptRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void titleTooLong_hasViolation() {
            String longTitle = "x".repeat(201);
            UpdateConceptRequest request = new UpdateConceptRequest(longTitle, null, null);
            Set<ConstraintViolation<UpdateConceptRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- CreateCardRequest ---

    @Nested
    class CreateCardRequestValidation {

        @Test
        void valid_noViolations() {
            CreateCardRequest request = new CreateCardRequest(UUID.randomUUID(), "Q?", "A!", CardType.STANDARD, null, null);
            Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullConceptId_hasViolation() {
            CreateCardRequest request = new CreateCardRequest(null, "Q?", "A!", CardType.STANDARD, null, null);
            Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankFront_hasViolation() {
            CreateCardRequest request = new CreateCardRequest(UUID.randomUUID(), "", "A!", null, null, null);
            Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankBack_hasViolation() {
            CreateCardRequest request = new CreateCardRequest(UUID.randomUUID(), "Q?", "", null, null, null);
            Set<ConstraintViolation<CreateCardRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- UpdateCardRequest ---

    @Nested
    class UpdateCardRequestValidation {

        @Test
        void valid_noViolations() {
            UpdateCardRequest request = new UpdateCardRequest("Q?", "A!", CardType.COMPARE, null, null);
            Set<ConstraintViolation<UpdateCardRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankFront_hasViolation() {
            UpdateCardRequest request = new UpdateCardRequest("", "A!", null, null, null);
            Set<ConstraintViolation<UpdateCardRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankBack_hasViolation() {
            UpdateCardRequest request = new UpdateCardRequest("Q?", "", null, null, null);
            Set<ConstraintViolation<UpdateCardRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- SubmitReviewRequest ---

    @Nested
    class SubmitReviewRequestValidation {

        @Test
        void valid_noViolations() {
            SubmitReviewRequest request = new SubmitReviewRequest(3, 1000L, 2);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullRating_hasViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(null, null, null);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void ratingTooLow_hasViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(-1, null, null);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void ratingTooHigh_hasViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(6, null, null);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void ratingAtBounds_noViolation() {
            assertTrue(validator.validate(new SubmitReviewRequest(0, null, null)).isEmpty());
            assertTrue(validator.validate(new SubmitReviewRequest(5, null, null)).isEmpty());
        }

        @Test
        void confidenceTooLow_hasViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(3, null, 0);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void confidenceTooHigh_hasViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(3, null, 4);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullConfidence_noViolation() {
            SubmitReviewRequest request = new SubmitReviewRequest(3, null, null);
            Set<ConstraintViolation<SubmitReviewRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }
    }

    // --- SubmitTeachBackRequest ---

    @Nested
    class SubmitTeachBackRequestValidation {

        @Test
        void valid_noViolations() {
            SubmitTeachBackRequest request = new SubmitTeachBackRequest(
                    UUID.randomUUID(), "My explanation", 4, List.of());
            Set<ConstraintViolation<SubmitTeachBackRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullConceptId_hasViolation() {
            SubmitTeachBackRequest request = new SubmitTeachBackRequest(null, "explanation", 3, null);
            Set<ConstraintViolation<SubmitTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankExplanation_hasViolation() {
            SubmitTeachBackRequest request = new SubmitTeachBackRequest(UUID.randomUUID(), "", 3, null);
            Set<ConstraintViolation<SubmitTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void selfRatingTooLow_hasViolation() {
            SubmitTeachBackRequest request = new SubmitTeachBackRequest(UUID.randomUUID(), "explain", 0, null);
            Set<ConstraintViolation<SubmitTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void selfRatingTooHigh_hasViolation() {
            SubmitTeachBackRequest request = new SubmitTeachBackRequest(UUID.randomUUID(), "explain", 6, null);
            Set<ConstraintViolation<SubmitTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- GenerateCardsRequest ---

    @Nested
    class GenerateCardsRequestValidation {

        @Test
        void valid_noViolations() {
            GenerateCardsRequest request = new GenerateCardsRequest(UUID.randomUUID(), "content", 5);
            Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullConceptId_hasViolation() {
            GenerateCardsRequest request = new GenerateCardsRequest(null, "content", 3);
            Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankContent_hasViolation() {
            GenerateCardsRequest request = new GenerateCardsRequest(UUID.randomUUID(), "", 3);
            Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void numCardsTooLow_hasViolation() {
            GenerateCardsRequest request = new GenerateCardsRequest(UUID.randomUUID(), "content", 0);
            Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void numCardsTooHigh_hasViolation() {
            GenerateCardsRequest request = new GenerateCardsRequest(UUID.randomUUID(), "content", 21);
            Set<ConstraintViolation<GenerateCardsRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- EvaluateTeachBackRequest ---

    @Nested
    class EvaluateTeachBackRequestValidation {

        @Test
        void valid_noViolations() {
            EvaluateTeachBackRequest request = new EvaluateTeachBackRequest(UUID.randomUUID(), "explanation");
            Set<ConstraintViolation<EvaluateTeachBackRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullConceptId_hasViolation() {
            EvaluateTeachBackRequest request = new EvaluateTeachBackRequest(null, "explanation");
            Set<ConstraintViolation<EvaluateTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankExplanation_hasViolation() {
            EvaluateTeachBackRequest request = new EvaluateTeachBackRequest(UUID.randomUUID(), "");
            Set<ConstraintViolation<EvaluateTeachBackRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- ImportRequest ---

    @Nested
    class ImportRequestValidation {

        @Test
        void valid_noViolations() {
            ImportTopicData topicData = new ImportTopicData("Java", "desc", "#FF0000",
                    List.of(new ImportConceptData("OOP", null, null,
                            List.of(new ImportCardData("Q", "A", null, null, null)))));
            ImportRequest request = new ImportRequest("1.0", List.of(topicData));
            Set<ConstraintViolation<ImportRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void nullExportVersion_hasViolation() {
            ImportTopicData topicData = new ImportTopicData("Java", null, null, null);
            ImportRequest request = new ImportRequest(null, List.of(topicData));
            Set<ConstraintViolation<ImportRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void emptyTopicsList_hasViolation() {
            ImportRequest request = new ImportRequest("1.0", List.of());
            Set<ConstraintViolation<ImportRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullTopicsList_hasViolation() {
            ImportRequest request = new ImportRequest("1.0", null);
            Set<ConstraintViolation<ImportRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }

    // --- ImportTopicData ---

    @Nested
    class ImportTopicDataValidation {

        @Test
        void valid_noViolations() {
            ImportTopicData data = new ImportTopicData("Java", null, null, null);
            Set<ConstraintViolation<ImportTopicData>> violations = validator.validate(data);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankName_hasViolation() {
            ImportTopicData data = new ImportTopicData("", null, null, null);
            Set<ConstraintViolation<ImportTopicData>> violations = validator.validate(data);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nameTooLong_hasViolation() {
            ImportTopicData data = new ImportTopicData("x".repeat(101), null, null, null);
            Set<ConstraintViolation<ImportTopicData>> violations = validator.validate(data);
            assertFalse(violations.isEmpty());
        }
    }

    // --- ImportCardData ---

    @Nested
    class ImportCardDataValidation {

        @Test
        void valid_noViolations() {
            ImportCardData data = new ImportCardData("Q", "A", "STANDARD", null, null);
            Set<ConstraintViolation<ImportCardData>> violations = validator.validate(data);
            assertTrue(violations.isEmpty());
        }

        @Test
        void blankFront_hasViolation() {
            ImportCardData data = new ImportCardData("", "A", null, null, null);
            Set<ConstraintViolation<ImportCardData>> violations = validator.validate(data);
            assertFalse(violations.isEmpty());
        }

        @Test
        void blankBack_hasViolation() {
            ImportCardData data = new ImportCardData("Q", "", null, null, null);
            Set<ConstraintViolation<ImportCardData>> violations = validator.validate(data);
            assertFalse(violations.isEmpty());
        }
    }

    // --- ReorderRequest ---

    @Nested
    class ReorderRequestValidation {

        @Test
        void valid_noViolations() {
            ReorderRequest request = new ReorderRequest(List.of(UUID.randomUUID(), UUID.randomUUID()));
            Set<ConstraintViolation<ReorderRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty());
        }

        @Test
        void emptyList_hasViolation() {
            ReorderRequest request = new ReorderRequest(List.of());
            Set<ConstraintViolation<ReorderRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }

        @Test
        void nullList_hasViolation() {
            ReorderRequest request = new ReorderRequest(null);
            Set<ConstraintViolation<ReorderRequest>> violations = validator.validate(request);
            assertFalse(violations.isEmpty());
        }
    }
}
