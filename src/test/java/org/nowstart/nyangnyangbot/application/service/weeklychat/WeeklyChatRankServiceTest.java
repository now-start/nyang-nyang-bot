package org.nowstart.nyangnyangbot.application.service.weeklychat;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.user.ObserveUserUseCase;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatCountPort;

@ExtendWith(MockitoExtension.class)
class WeeklyChatRankServiceTest {

    @Mock
    private WeeklyChatCountPort weeklyChatCountPort;
    @Mock
    private ObserveUserUseCase observeUserUseCase;
    @Spy
    @InjectMocks
    private WeeklyChatRankService service;

    @Test
    void recordChat_ObservesUserBeforeAtomicIncrement() {
        given(service.currentDate()).willReturn(LocalDate.of(2026, 3, 25));
        ChatReceived chat = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", List.of(), true),
                "안녕",
                null,
                0L
        );

        service.recordChat(chat);

        var order = inOrder(observeUserUseCase, weeklyChatCountPort);
        order.verify(observeUserUseCase).observeUser("user-1", "치즈냥");
        order.verify(weeklyChatCountPort).increment(LocalDate.of(2026, 3, 23), "user-1");
    }

    @Test
    void getWeeklyRanks_ReturnsAtomicAggregateProjection() {
        given(service.currentDate()).willReturn(LocalDate.of(2026, 3, 26));
        given(weeklyChatCountPort.findWeeklyRanks(LocalDate.of(2026, 3, 23), 2)).willReturn(List.of(
                new WeeklyChatRankView(1, "치즈냥", 22L),
                new WeeklyChatRankView(2, "고양이", 18L)
        ));

        then(service.getWeeklyRanks(2)).hasSize(2);
    }
}
