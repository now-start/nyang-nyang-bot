package org.nowstart.nyangnyangbot.application.service.weeklychat;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;

@ExtendWith(MockitoExtension.class)
class WeeklyChatRankServiceTest {

    @Mock
    private WeeklyChatRankPort weeklyChatRankPort;

    @Spy
    @InjectMocks
    private WeeklyChatRankService weeklyChatRankService;

    @Test
    void recordChat_ShouldCreateNewWeeklyRank_WhenFirstMessageOfWeek() {
        // 준비
        LocalDate now = LocalDate.of(2026, 3, 25);
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", List.of(), true),
                "안녕",
                null,
                0L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankPort.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-1"))
                .willReturn(Optional.empty());

        // 실행
        weeklyChatRankService.recordChat(chatDto);

        // 검증
        BDDMockito.then(weeklyChatRankPort).should().save(argThat(entity ->
                LocalDate.of(2026, 3, 23).equals(entity.weekStartDate())
                        && "user-1".equals(entity.userId())
                        && "치즈냥".equals(entity.nickName())
                        && Long.valueOf(1L).equals(entity.chatCount())
        ));
    }

    @Test
    void recordChat_ShouldIncrementExistingWeeklyRank_AndRefreshNickname() {
        // 준비
        LocalDate now = LocalDate.of(2026, 3, 26);
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("새닉네임", List.of(), true),
                "두번째",
                null,
                0L
        );
        WeeklyChatRankRecordResult existing = new WeeklyChatRankRecordResult(
                1L,
                LocalDate.of(2026, 3, 23),
                "user-1",
                "이전닉네임",
                4L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankPort.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-1"))
                .willReturn(Optional.of(existing));

        // 실행
        weeklyChatRankService.recordChat(chatDto);

        // 검증
        BDDMockito.then(weeklyChatRankPort).should().save(argThat(entity ->
                Long.valueOf(1L).equals(entity.id())
                        && "새닉네임".equals(entity.nickName())
                        && Long.valueOf(5L).equals(entity.chatCount())
        ));
    }

    @Test
    void recordChat_ShouldUseUserId_WhenNicknameMissing() {
        // 준비
        LocalDate now = LocalDate.of(2026, 3, 26);
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-42",
                new ChatReceived.Profile("", List.of(), false),
                "세번째",
                null,
                0L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankPort.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-42"))
                .willReturn(Optional.empty());

        // 실행
        weeklyChatRankService.recordChat(chatDto);

        // 검증
        BDDMockito.then(weeklyChatRankPort).should().save(argThat(entity ->
                "user-42".equals(entity.nickName())
                        && Long.valueOf(1L).equals(entity.chatCount())
        ));
    }

    @Test
    void recordChat_ShouldUseUserId_WhenProfileMissing() {
        // 준비
        LocalDate now = LocalDate.of(2026, 3, 26);
        ChatReceived chatDto = new ChatReceived(
                "channel-1",
                "user-43",
                null,
                "네번째",
                null,
                0L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankPort.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-43"))
                .willReturn(Optional.empty());

        // 실행
        weeklyChatRankService.recordChat(chatDto);

        // 검증
        BDDMockito.then(weeklyChatRankPort).should().save(argThat(entity ->
                "user-43".equals(entity.nickName())
                        && Long.valueOf(1L).equals(entity.chatCount())
        ));
    }

    @Test
    void getWeeklyRanks_ShouldMapRepositoryResultsToRankedDtos() {
        // 준비
        LocalDate now = LocalDate.of(2026, 3, 26);
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankPort.findWeeklyRanks(eq(LocalDate.of(2026, 3, 23)), eq(2)))
                .willReturn(List.of(
                        new WeeklyChatRankView(1, "치즈냥", 22L),
                        new WeeklyChatRankView(2, "고양이", 18L)
                ));

        // 실행
        List<WeeklyChatRankView> result = weeklyChatRankService.getWeeklyRanks(2);

        // 검증
        then(result).containsExactly(
                new WeeklyChatRankView(1, "치즈냥", 22L),
                new WeeklyChatRankView(2, "고양이", 18L)
        );
    }

}
