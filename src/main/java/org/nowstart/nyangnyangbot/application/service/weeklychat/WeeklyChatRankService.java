package org.nowstart.nyangnyangbot.application.service.weeklychat;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.ChatEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyChatRankService implements QueryWeeklyChatRankUseCase, RecordWeeklyChatUseCase {

    private final WeeklyChatRankPort weeklyChatRankPort;

    @Override
    public synchronized void recordChat(ChatEventPayload chat) {
        if (chat == null || StringUtils.isBlank(chat.senderChannelId())) {
            return;
        }

        String nickName = resolveNickname(chat);
        LocalDate weekStartDate = currentWeekStartDate();
        WeeklyChatRankRecordResult existing = weeklyChatRankPort.findByWeekStartDateAndUserId(weekStartDate, chat.senderChannelId())
                .orElse(new WeeklyChatRankRecordResult(null, weekStartDate, chat.senderChannelId(), nickName, 0L));
        weeklyChatRankPort.save(new WeeklyChatRankRecordResult(
                existing.id(),
                weekStartDate,
                chat.senderChannelId(),
                nickName,
                Objects.requireNonNullElse(existing.chatCount(), 0L) + 1L
        ));
    }

    @Override
    public List<WeeklyChatRankView> getWeeklyRanks(int limit) {
        return weeklyChatRankPort.findWeeklyRanks(currentWeekStartDate(), limit);
    }

    LocalDate currentDate() {
        return LocalDate.now();
    }

    private LocalDate currentWeekStartDate() {
        return currentDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private String resolveNickname(ChatEventPayload chat) {
        if (chat.profile() != null && !StringUtils.isBlank(chat.profile().nickname())) {
            return chat.profile().nickname();
        }
        return chat.senderChannelId();
    }
}
