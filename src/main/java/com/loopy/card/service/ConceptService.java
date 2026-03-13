package com.loopy.card.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.ConceptResponse;
import com.loopy.card.dto.CreateConceptRequest;
import com.loopy.card.dto.UpdateConceptRequest;
import com.loopy.card.entity.Concept;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import com.loopy.topic.entity.Topic;
import com.loopy.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConceptService {

    private final ConceptRepository conceptRepository;
    private final TopicRepository topicRepository;

    public ConceptService(ConceptRepository conceptRepository, TopicRepository topicRepository) {
        this.conceptRepository = conceptRepository;
        this.topicRepository = topicRepository;
    }

    public List<ConceptResponse> getConcepts(UUID topicId, User user) {
        return conceptRepository.findByTopicIdAndUserIdOrderByTitleAsc(topicId, user.getId()).stream()
                .map(ConceptResponse::from)
                .toList();
    }

    public ConceptResponse getConcept(UUID id, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));
        return ConceptResponse.from(concept);
    }

    @Transactional
    public ConceptResponse createConcept(CreateConceptRequest request, User user) {
        Topic topic = topicRepository.findByIdAndUserId(request.topicId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found"));

        Concept concept = new Concept(topic, user, request.title(), request.notes());
        return ConceptResponse.from(conceptRepository.save(concept));
    }

    @Transactional
    public ConceptResponse updateConcept(UUID id, UpdateConceptRequest request, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        concept.setTitle(request.title());
        concept.setNotes(request.notes());
        concept.setReferenceExplanation(request.referenceExplanation());

        return ConceptResponse.from(conceptRepository.save(concept));
    }

    @Transactional
    public void deleteConcept(UUID id, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));
        conceptRepository.delete(concept);
    }
}
