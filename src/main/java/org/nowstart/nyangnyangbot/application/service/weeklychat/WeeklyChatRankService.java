package org.nowstart.nyangnyangbot.application.service.weeklychat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.RecordWeeklyChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;
import org.nowstart.nyangnyangbot.application.service.chat.ChatEventSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklyChatRankService implements QueryWeeklyChatRankUseCase, RecordWeeklyChatUseCase {

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
        LocalDate weekStartDate = currentWeekStartDate();
        observeUserUseCase.observeUser(userId, nickName);
        weeklyChatCountPort.increment(weekStartDate, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyChatRankView> getWeeklyRanks(int limit) {
        return weeklyChatCountPort.findWeeklyRanks(currentWeekStartDate(), limit);
    }

    LocalDate currentDate() {
        return LocalDate.now();
    }

    private LocalDate currentWeekStartDate() {
        return currentDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
