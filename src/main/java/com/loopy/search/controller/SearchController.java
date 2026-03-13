package com.loopy.search.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @RequestParam, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.search.dto.SearchResponse;
import com.loopy.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(value = "q", defaultValue = "") String query,
            @AuthenticationPrincipal User user) {

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return ResponseEntity.ok(new SearchResponse(List.of(), List.of(), List.of()));
        }

        return ResponseEntity.ok(searchService.search(trimmed, user));
    }
}
