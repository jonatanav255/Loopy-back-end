package com.loopy.teachback.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.entity.Concept;
import com.loopy.card.entity.ConceptStatus;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.teachback.dto.SubmitTeachBackRequest;
import com.loopy.teachback.dto.TeachBackResponse;
import com.loopy.teachback.entity.TeachBack;
import com.loopy.teachback.repository.TeachBackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeachBackService {

    private final TeachBackRepository teachBackRepository;
    private final ConceptRepository conceptRepository;

    public TeachBackService(TeachBackRepository teachBackRepository,
                            ConceptRepository conceptRepository) {
        this.teachBackRepository = teachBackRepository;
        this.conceptRepository = conceptRepository;
    }

    /** Returns concepts that require teach-back for this user. */
    public List<ConceptResponse> getConceptsRequiringTeachBack(User user) {
        return conceptRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                        user.getId(), ConceptStatus.TEACH_BACK_REQUIRED).stream()
                .map(ConceptResponse::from)
                .toList();
    }

    /** Submits a teach-back session for a concept. */
    @Transactional
    public TeachBackResponse submitTeachBack(SubmitTeachBackRequest request, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(request.conceptId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        String gapsJoined = request.gapsFound() != null && !request.gapsFound().isEmpty()
                ? String.join("|", request.gapsFound())
                : null;

        TeachBack tb = new TeachBack(concept, user, request.userExplanation(),
                request.selfRating(), gapsJoined);
        teachBackRepository.save(tb);

        // If self-rating is high enough, move concept back to REVIEW status
        if (request.selfRating() >= 4) {
            concept.setStatus(ConceptStatus.REVIEW);
            conceptRepository.save(concept);
        }

        return TeachBackResponse.from(tb);
    }

    /** Returns teach-back history for a concept. */
    public List<TeachBackResponse> getHistory(UUID conceptId, User user) {
        conceptRepository.findByIdAndUserId(conceptId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        return teachBackRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, user.getId())
                .stream()
                .map(TeachBackResponse::from)
                .toList();
    }
}
