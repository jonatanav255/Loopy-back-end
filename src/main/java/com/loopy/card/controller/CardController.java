package com.loopy.card.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @Valid, @RequestBody, @PathVariable, @RequestParam, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.dto.CreateCardRequest;
import com.loopy.card.dto.UpdateCardRequest;
import com.loopy.card.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> list(@RequestParam UUID conceptId,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cardService.getCards(conceptId, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> get(@PathVariable UUID id,
                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cardService.getCard(id, user));
    }

    @PostMapping
    public ResponseEntity<CardResponse> create(@Valid @RequestBody CreateCardRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateCardRequest request,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cardService.updateCard(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal User user) {
        cardService.deleteCard(id, user);
        return ResponseEntity.noContent().build();
    }
}
