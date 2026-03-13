package com.loopy.card.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.UpdateCardRequest;
import com.loopy.card.entity.Card;
import com.loopy.card.entity.CardType;
import com.loopy.card.entity.Concept;
import com.loopy.review.service.SchedulingAlgorithm;
import com.loopy.card.repository.CardRepository;
import com.loopy.card.repository.ConceptRepository;
import com.loopy.config.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final ConceptRepository conceptRepository;

    public CardService(CardRepository cardRepository, ConceptRepository conceptRepository) {
        this.cardRepository = cardRepository;
        this.conceptRepository = conceptRepository;
    }

    public List<CardResponse> getCards(UUID conceptId, User user) {
        return cardRepository.findByConceptIdAndUserIdOrderByCreatedAtDesc(conceptId, user.getId()).stream()
                .map(CardResponse::from)
                .toList();
    }

    public CardResponse getCard(UUID id, User user) {
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        return CardResponse.from(card);
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request, User user) {
        Concept concept = conceptRepository.findByIdAndUserId(request.conceptId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Concept not found"));

        Card card = new Card(
                concept,
                user,
                request.front(),
                request.back(),
                request.cardType() != null ? request.cardType() : CardType.STANDARD,
                request.hint(),
                request.sourceUrl()
        );
        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public CardResponse updateCard(UUID id, UpdateCardRequest request, User user) {
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        card.setFront(request.front());
        card.setBack(request.back());
        if (request.cardType() != null) {
            card.setCardType(request.cardType());
        }
        card.setHint(request.hint());
        card.setSourceUrl(request.sourceUrl());

        return CardResponse.from(cardRepository.save(card));
    }

    @Transactional
    public void deleteCard(UUID id, User user) {
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        cardRepository.delete(card);
    }

    @Transactional
    public CardResponse switchAlgorithm(UUID id, SchedulingAlgorithm algorithm, User user) {
        Card card = cardRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        card.setSchedulingAlgorithm(algorithm);
        return CardResponse.from(cardRepository.save(card));
    }
}
