package org.nowstart.nyangnyangbot.application.service.weeklychat;

import jakarta.transaction.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;
import org.nowstart.nyangnyangbot.application.service.chat.ChatEventSupport;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyChatRankService implements QueryWeeklyChatRankUseCase, RecordWeeklyChatUseCase {

    private final WeeklyChatRankPort weeklyChatRankPort;

    @Override
    public synchronized void recordChat(ChatReceived chat) {
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }

        String userId = ChatEventSupport.senderChannelId(chat);
        String nickName = ChatEventSupport.displayName(chat);
        LocalDate weekStartDate = currentWeekStartDate();
        WeeklyChatRankRecordResult existing = weeklyChatRankPort.findByWeekStartDateAndUserId(weekStartDate, userId)
                .orElse(new WeeklyChatRankRecordResult(null, weekStartDate, userId, nickName, 0L));
        weeklyChatRankPort.save(new WeeklyChatRankRecordResult(
                existing.id(),
                weekStartDate,
                userId,
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
}
