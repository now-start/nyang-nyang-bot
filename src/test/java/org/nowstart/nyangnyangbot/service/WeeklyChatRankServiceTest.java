package org.nowstart.nyangnyangbot.service;

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
import org.nowstart.nyangnyangbot.data.dto.WeeklyChatRankDto;
import org.nowstart.nyangnyangbot.data.dto.chzzk.ChatDto;
import org.nowstart.nyangnyangbot.data.entity.WeeklyChatRankEntity;
import org.nowstart.nyangnyangbot.repository.WeeklyChatRankRepository;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WeeklyChatRankServiceTest {

    @Mock
    private WeeklyChatRankRepository weeklyChatRankRepository;

    @Spy
    @InjectMocks
    private WeeklyChatRankService weeklyChatRankService;

    @Test
    void recordChat_ShouldCreateNewWeeklyRank_WhenFirstMessageOfWeek() {
        LocalDate now = LocalDate.of(2026, 3, 25);
        ChatDto chatDto = new ChatDto(
                "channel-1",
                "user-1",
                new ChatDto.Profile("치즈냥", List.of(), true),
                "안녕",
                null,
                0L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankRepository.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-1"))
                .willReturn(Optional.empty());

        weeklyChatRankService.recordChat(chatDto);

        BDDMockito.then(weeklyChatRankRepository).should().save(argThat(entity ->
                LocalDate.of(2026, 3, 23).equals(entity.getWeekStartDate())
                        && "user-1".equals(entity.getUserId())
                        && "치즈냥".equals(entity.getNickName())
                        && Long.valueOf(1L).equals(entity.getChatCount())
        ));
    }

    @Test
    void recordChat_ShouldIncrementExistingWeeklyRank_AndRefreshNickname() {
        LocalDate now = LocalDate.of(2026, 3, 26);
        ChatDto chatDto = new ChatDto(
                "channel-1",
                "user-1",
                new ChatDto.Profile("새닉네임", List.of(), true),
                "두번째",
                null,
                0L
        );
        WeeklyChatRankEntity existing = WeeklyChatRankEntity.builder()
                .weekStartDate(LocalDate.of(2026, 3, 23))
                .userId("user-1")
                .nickName("이전닉네임")
                .chatCount(4L)
                .build();
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankRepository.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-1"))
                .willReturn(Optional.of(existing));

        weeklyChatRankService.recordChat(chatDto);

        BDDMockito.then(weeklyChatRankRepository).should().save(argThat(entity ->
                entity == existing
                        && "새닉네임".equals(entity.getNickName())
                        && Long.valueOf(5L).equals(entity.getChatCount())
        ));
    }

    @Test
    void recordChat_ShouldUseUserId_WhenNicknameMissing() {
        LocalDate now = LocalDate.of(2026, 3, 26);
        ChatDto chatDto = new ChatDto(
                "channel-1",
                "user-42",
                new ChatDto.Profile("", List.of(), false),
                "세번째",
                null,
                0L
        );
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankRepository.findByWeekStartDateAndUserId(LocalDate.of(2026, 3, 23), "user-42"))
                .willReturn(Optional.empty());

        weeklyChatRankService.recordChat(chatDto);

        BDDMockito.then(weeklyChatRankRepository).should().save(argThat(entity ->
                "user-42".equals(entity.getNickName())
                        && Long.valueOf(1L).equals(entity.getChatCount())
        ));
    }

    @Test
    void getWeeklyRanks_ShouldMapRepositoryResultsToRankedDtos() {
        LocalDate now = LocalDate.of(2026, 3, 26);
        WeeklyChatRankRepository.WeeklyChatRankProjection first = new Projection("치즈냥", 22L);
        WeeklyChatRankRepository.WeeklyChatRankProjection second = new Projection("고양이", 18L);
        given(weeklyChatRankService.currentDate()).willReturn(now);
        given(weeklyChatRankRepository.findWeeklyRanks(eq(LocalDate.of(2026, 3, 23)), argThat((Pageable pageable) ->
                pageable.getPageNumber() == 0 && pageable.getPageSize() == 2
        ))).willReturn(List.of(first, second));

        List<WeeklyChatRankDto> result = weeklyChatRankService.getWeeklyRanks(2);

        then(result).containsExactly(
                new WeeklyChatRankDto(1, "치즈냥", 22L),
                new WeeklyChatRankDto(2, "고양이", 18L)
        );
    }

    private record Projection(String nickname, Long chatCount) implements WeeklyChatRankRepository.WeeklyChatRankProjection {
        @Override
        public String getNickname() {
            return nickname;
        }

        @Override
        public Long getChatCount() {
            return chatCount;
        }
    }
}
