package org.nowstart.nyangnyangbot.service;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.data.dto.WeeklyChatRankDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.entity.WeeklyChatRankEntity;
import org.nowstart.nyangnyangbot.repository.WeeklyChatRankRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyChatRankService {

    private static final int MAX_LIMIT = 10;

    private final WeeklyChatRankRepository weeklyChatRankRepository;

    public synchronized void recordChat(ChatDto chatDto) {
        if (chatDto == null || StringUtils.isBlank(chatDto.senderChannelId())) {
            return;
        }

        String nickName = resolveNickname(chatDto);
        LocalDate weekStartDate = currentWeekStartDate();
        WeeklyChatRankEntity entity = weeklyChatRankRepository.findByWeekStartDateAndUserId(weekStartDate, chatDto.senderChannelId())
                .orElseGet(() -> WeeklyChatRankEntity.builder()
                        .weekStartDate(weekStartDate)
                        .userId(chatDto.senderChannelId())
                        .nickName(nickName)
                        .chatCount(0L)
                        .build());

        entity.setNickName(nickName);
        entity.setChatCount(Objects.requireNonNullElse(entity.getChatCount(), 0L) + 1L);
        weeklyChatRankRepository.save(entity);
    }

    public List<WeeklyChatRankDto> getWeeklyRanks(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        List<WeeklyChatRankRepository.WeeklyChatRankProjection> results =
                weeklyChatRankRepository.findWeeklyRanks(currentWeekStartDate(), PageRequest.of(0, safeLimit));

        List<WeeklyChatRankDto> ranks = new ArrayList<>(results.size());
        int rank = 1;
        for (WeeklyChatRankRepository.WeeklyChatRankProjection result : results) {
            long chatCount = result.getChatCount() == null ? 0L : result.getChatCount();
            ranks.add(new WeeklyChatRankDto(rank++, result.getNickname(), chatCount));
        }
        return ranks;
    }

    LocalDate currentDate() {
        return LocalDate.now();
    }

    private LocalDate currentWeekStartDate() {
        return currentDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String resolveNickname(ChatDto chatDto) {
        if (chatDto.profile() != null && !StringUtils.isBlank(chatDto.profile().nickname())) {
            return chatDto.profile().nickname();
        }
        return chatDto.senderChannelId();
    }
}
