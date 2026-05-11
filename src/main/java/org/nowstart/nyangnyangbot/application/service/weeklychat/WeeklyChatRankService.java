package org.nowstart.nyangnyangbot.application.service.weeklychat;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.dto.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.dto.ChatDto;
import org.nowstart.nyangnyangbot.application.port.out.weekly.repository.WeeklyChatRankPort;
import org.nowstart.nyangnyangbot.domain.model.WeeklyChatRankRecord;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyChatRankService {

    private final WeeklyChatRankPort weeklyChatRankPort;

    public synchronized void recordChat(ChatDto chatDto) {
        if (chatDto == null || StringUtils.isBlank(chatDto.senderChannelId())) {
            return;
        }

        String nickName = resolveNickname(chatDto);
        LocalDate weekStartDate = currentWeekStartDate();
        WeeklyChatRankRecord existing = weeklyChatRankPort.findByWeekStartDateAndUserId(weekStartDate, chatDto.senderChannelId())
                .orElse(new WeeklyChatRankRecord(null, weekStartDate, chatDto.senderChannelId(), nickName, 0L));
        weeklyChatRankPort.save(new WeeklyChatRankRecord(
                existing.id(),
                weekStartDate,
                chatDto.senderChannelId(),
                nickName,
                Objects.requireNonNullElse(existing.chatCount(), 0L) + 1L
        ));
    }

    public List<WeeklyChatRankView> getWeeklyRanks(int limit) {
        return weeklyChatRankPort.findWeeklyRanks(currentWeekStartDate(), limit);
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
