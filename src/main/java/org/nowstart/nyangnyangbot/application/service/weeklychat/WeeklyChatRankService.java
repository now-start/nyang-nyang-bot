package org.nowstart.nyangnyangbot.application.service.weeklychat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort.IncrementWeeklyChatCommand;
import org.nowstart.nyangnyangbot.application.service.chat.ChatEventSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class WeeklyChatRankService implements QueryWeeklyChatRankUseCase, RecordWeeklyChatUseCase {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final WeeklyChatCountPort weeklyChatCountPort;
    private final ObserveUserUseCase observeUserUseCase;

    @Override
    @Transactional
    public void recordChat(ChatReceived chat) {
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }

        String userId = ChatEventSupport.senderChannelId(chat);
        String nickName = ChatEventSupport.displayName(chat);
        Instant weekStartedAt = currentWeekStartedAt();
        observeUserUseCase.observeUser(userId, nickName);
        weeklyChatCountPort.increment(new IncrementWeeklyChatCommand(weekStartedAt, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyChatRankView> getWeeklyRanks(int limit) {
        return weeklyChatCountPort.findWeeklyRanks(currentWeekStartedAt(), limit).stream()
                .map(rank -> new WeeklyChatRankView(rank.rank(), rank.displayName(), rank.chatCount()))
                .toList();
    }

    Instant currentTime() {
        return weeklyChatCountPort.currentDatabaseTime();
    }

    private Instant currentWeekStartedAt() {
        LocalDate weekStartDate = currentTime()
                .atZone(SEOUL)
                .toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return weekStartDate.atStartOfDay(SEOUL).toInstant();
    }
}
