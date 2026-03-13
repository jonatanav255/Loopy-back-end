package com.loopy.topic.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @Valid, @RequestBody, @PathVariable, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.config.ReorderRequest;
import com.loopy.topic.dto.CreateTopicRequest;
import com.loopy.topic.dto.TopicResponse;
import com.loopy.topic.dto.UpdateTopicRequest;
import com.loopy.topic.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ResponseEntity<List<TopicResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(topicService.getTopics(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TopicResponse> get(@PathVariable UUID id,
                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(topicService.getTopic(id, user));
    }

    @PostMapping
    public ResponseEntity<TopicResponse> create(@Valid @RequestBody CreateTopicRequest request,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(request, user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TopicResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateTopicRequest request,
                                                @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(topicService.updateTopic(id, request, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal User user) {
        topicService.deleteTopic(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<List<TopicResponse>> reorder(@Valid @RequestBody ReorderRequest request,
                                                       @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(topicService.reorderTopics(request, user));
    }
}
