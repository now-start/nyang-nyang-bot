package org.nowstart.nyangnyangbot.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.DonationRankDto;
import org.nowstart.nyangnyangbot.repository.DonationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DonationRankService {

    private static final int MAX_LIMIT = 50;
    private final DonationRepository donationRepository;

    public List<DonationRankDto> getWeeklyRanks(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndExclusive = weekStart.plusDays(7);
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekEndExclusive.atStartOfDay();

        List<DonationRepository.DonationRankProjection> results =
                donationRepository.findWeeklyRanks(from, to, PageRequest.of(0, safeLimit));

        List<DonationRankDto> ranks = new ArrayList<>(results.size());
        int rank = 1;
        for (DonationRepository.DonationRankProjection result : results) {
            long totalAmount = result.getTotalAmount() == null ? 0L : result.getTotalAmount();
            ranks.add(new DonationRankDto(rank++, result.getNickname(), totalAmount));
        }
        return ranks;
    }
}
