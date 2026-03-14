package com.loopy.review.service;

// Dependencies: @Service, @Transactional — see DEPENDENCY_GUIDE.md
import com.loopy.auth.entity.User;
import com.loopy.card.dto.CardResponse;
import com.loopy.card.repository.CardRepository;
import com.loopy.review.dto.*;
import com.loopy.review.entity.ReviewLog;
import com.loopy.review.repository.ReviewLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ReviewLogRepository reviewLogRepository;
    private final CardRepository cardRepository;
    public StatsService(ReviewLogRepository reviewLogRepository,
                        CardRepository cardRepository) {
        this.reviewLogRepository = reviewLogRepository;
        this.cardRepository = cardRepository;
    }

    /** Returns high-level overview stats for the dashboard. */
    public StatsOverview getOverview(User user) {
        UUID userId = user.getId();
        LocalDate today = LocalDate.now();

        int cardsDue = (int) cardRepository.countDueCards(userId, today);
        int totalCards = (int) cardRepository.countByUserId(userId);

        Instant dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        long reviewedToday = reviewLogRepository.countByUserIdAndReviewedAtBetween(userId, dayStart, dayEnd);
        long passedToday = reviewLogRepository.countPassedByUserInRange(userId, dayStart, dayEnd);

        double accuracy = reviewedToday == 0 ? 0.0 : (double) passedToday / reviewedToday * 100.0;

        int[] streaks = calculateStreaks(userId);

        return new StatsOverview(
                cardsDue,
                (int) reviewedToday,
                totalCards,
                Math.round(accuracy * 10.0) / 10.0,
                streaks[0],
                streaks[1]
        );
    }

/** Returns daily review counts for a heatmap (last 365 days). */
    public List<HeatmapEntry> getHeatmap(User user) {
        List<ReviewLog> reviews = reviewLogRepository.findAllByUserIdOrdered(user.getId());
        LocalDate cutoff = LocalDate.now().minusDays(365);

        Map<LocalDate, Long> counts = reviews.stream()
                .map(r -> r.getReviewedAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .filter(d -> !d.isBefore(cutoff))
                .collect(Collectors.groupingBy(d -> d, Collectors.counting()));

        return counts.entrySet().stream()
                .map(e -> new HeatmapEntry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(HeatmapEntry::date))
                .toList();
    }

    /** Returns cards flagged as fragile knowledge (correct + low confidence). */
    public List<FragileCard> getFragileCards(User user) {
        List<ReviewLog> fragileReviews = reviewLogRepository.findFragileReviews(user.getId());

        // Group by card ID and count occurrences, keep the most recent review
        Map<UUID, List<ReviewLog>> byCard = fragileReviews.stream()
                .collect(Collectors.groupingBy(r -> r.getCard().getId()));

        List<FragileCard> result = new ArrayList<>();
        for (Map.Entry<UUID, List<ReviewLog>> entry : byCard.entrySet()) {
            List<ReviewLog> cardReviews = entry.getValue();
            ReviewLog mostRecent = cardReviews.get(0); // already sorted DESC
            result.add(new FragileCard(
                    CardResponse.from(mostRecent.getCard()),
                    mostRecent.getRating(),
                    mostRecent.getConfidence(),
                    cardReviews.size()
            ));
        }

        // Sort by occurrences descending (most fragile first)
        result.sort(Comparator.comparingLong(FragileCard::occurrences).reversed());
        return result;
    }

    /**
     * Calculates current streak and longest streak.
     * A streak is consecutive days with at least one review.
     * Returns [currentStreak, longestStreak].
     */
    private int[] calculateStreaks(UUID userId) {
        List<ReviewLog> reviews = reviewLogRepository.findAllByUserIdOrdered(userId);
        if (reviews.isEmpty()) return new int[]{0, 0};

        // Get distinct review dates
        List<LocalDate> dates = reviews.stream()
                .map(r -> r.getReviewedAt().atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct()
                .sorted()
                .toList();

        int currentStreak = 1;
        int longestStreak = 1;
        int tempStreak = 1;

        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i - 1).plusDays(1))) {
                tempStreak++;
            } else {
                tempStreak = 1;
            }
            longestStreak = Math.max(longestStreak, tempStreak);
        }

        // Current streak: check if the last review date is today or yesterday
        LocalDate lastDate = dates.get(dates.size() - 1);
        LocalDate today = LocalDate.now();

        if (lastDate.equals(today) || lastDate.equals(today.minusDays(1))) {
            // Count backwards from the last date
            currentStreak = 1;
            for (int i = dates.size() - 2; i >= 0; i--) {
                if (dates.get(i).equals(dates.get(i + 1).minusDays(1))) {
                    currentStreak++;
                } else {
                    break;
                }
            }
        } else {
            currentStreak = 0;
        }

        return new int[]{currentStreak, longestStreak};
    }
}
