package com.loopy.review.controller;

// Dependencies: @RestController, @RequestMapping, @GetMapping, @AuthenticationPrincipal, ResponseEntity — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.review.dto.FragileCard;
import com.loopy.review.dto.HeatmapEntry;
import com.loopy.review.dto.StatsOverview;
import com.loopy.review.dto.TopicAccuracy;
import com.loopy.review.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** Dashboard overview: due count, reviewed today, accuracy, streaks. */
    @GetMapping("/overview")
    public ResponseEntity<StatsOverview> overview(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getOverview(user));
    }

    /** Accuracy breakdown by topic. */
    @GetMapping("/accuracy")
    public ResponseEntity<List<TopicAccuracy>> accuracy(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getAccuracyByTopic(user));
    }

    /** Daily review counts for activity heatmap (last 365 days). */
    @GetMapping("/heatmap")
    public ResponseEntity<List<HeatmapEntry>> heatmap(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getHeatmap(user));
    }

    /** Cards flagged as fragile knowledge (correct but low confidence). */
    @GetMapping("/fragile")
    public ResponseEntity<List<FragileCard>> fragile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statsService.getFragileCards(user));
    }
}
