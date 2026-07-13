package org.nowstart.nyangnyangbot.adapter.out.persistence.weekly;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.nowstart.nyangnyangbot.support.OutboundContractTestSupport.outboundContractValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.entity.WeeklyChatRank;
import org.nowstart.nyangnyangbot.adapter.out.persistence.weekly.repository.WeeklyChatRankRepository;
import org.nowstart.nyangnyangbot.application.port.in.weeklychat.QueryWeeklyChatRankUseCase.WeeklyChatRankView;
import org.nowstart.nyangnyangbot.application.port.out.weekly.WeeklyChatRankPort.WeeklyChatRankRecordResult;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class WeeklyChatRankPersistenceAdapterTest {

    @Mock
    private WeeklyChatRankRepository weeklyChatRankRepository;

    @Test
    void findAndSave_ShouldMapWeeklyRankRecord() {
        // 준비
        WeeklyChatRankPersistenceAdapter adapter = adapter();
        LocalDate weekStartDate = LocalDate.of(2026, 5, 11);
        WeeklyChatRank entity = rank(1L, weekStartDate, "user-1", "치즈냥", 3L);
        given(weeklyChatRankRepository.findByWeekStartDateAndUserId(weekStartDate, "user-1"))
                .willReturn(Optional.of(entity));
        given(weeklyChatRankRepository.findById(1L)).willReturn(Optional.of(entity));
        given(weeklyChatRankRepository.save(any(WeeklyChatRank.class))).willReturn(entity);

        // 실행
        WeeklyChatRankRecordResult found = adapter.findByWeekStartDateAndUserId(weekStartDate, "user-1").orElseThrow();
        WeeklyChatRankRecordResult saved = adapter.save(new WeeklyChatRankRecordResult(
                1L,
                weekStartDate,
                "user-1",
                "치즈냥",
                5L
        ));

        // 검증
        then(found.chatCount()).isEqualTo(3L);
        then(saved.id()).isEqualTo(1L);
        BDDMockito.then(weeklyChatRankRepository).should().save(entity);
    }

    @Test
    void save_ShouldCreateEntityWhenRecordHasNoIdOrExistingEntityIsMissing() {
        // 준비
        WeeklyChatRankPersistenceAdapter adapter = adapter();
        LocalDate weekStartDate = LocalDate.of(2026, 5, 11);
        WeeklyChatRank saved = rank(2L, weekStartDate, "user-2", "새냥", 7L);
        given(weeklyChatRankRepository.findById(404L)).willReturn(Optional.empty());
        given(weeklyChatRankRepository.save(any(WeeklyChatRank.class))).willReturn(saved);

        // 실행
        WeeklyChatRankRecordResult created = adapter.save(new WeeklyChatRankRecordResult(
                null,
                weekStartDate,
                "user-2",
                "새냥",
                7L
        ));
        WeeklyChatRankRecordResult recreated = adapter.save(new WeeklyChatRankRecordResult(
                404L,
                weekStartDate,
                "user-2",
                "새냥",
                7L
        ));

        // 검증
        then(created.id()).isEqualTo(2L);
        then(recreated.id()).isEqualTo(2L);
    }

    @Test
    void findWeeklyRanks_ShouldAssignRankAndFallbackNullChatCount() {
        // 준비
        WeeklyChatRankPersistenceAdapter adapter = adapter();
        LocalDate weekStartDate = LocalDate.of(2026, 5, 11);
        given(weeklyChatRankRepository.findWeeklyRanks(weekStartDate, PageRequest.of(0, 2)))
                .willReturn(List.of(
                        projection("치즈냥", 10L),
                        projection("새냥", null)
                ));

        // 실행
        List<WeeklyChatRankView> result = adapter.findWeeklyRanks(weekStartDate, 2);

        // 검증
        then(result).hasSize(2);
        then(result.get(0).rank()).isEqualTo(1);
        then(result.get(0).chatCount()).isEqualTo(10L);
        then(result.get(1).rank()).isEqualTo(2);
        then(result.get(1).chatCount()).isZero();
    }

    private WeeklyChatRank rank(
            Long id,
            LocalDate weekStartDate,
            String userId,
            String nickName,
            Long chatCount
    ) {
        return WeeklyChatRank.builder()
                .id(id)
                .weekStartDate(weekStartDate)
                .userId(userId)
                .nickName(nickName)
                .chatCount(chatCount)
                .build();
    }

    private WeeklyChatRankPersistenceAdapter adapter() {
        return new WeeklyChatRankPersistenceAdapter(
                weeklyChatRankRepository,
                outboundContractValidator()
        );
    }

    private WeeklyChatRankRepository.WeeklyChatRankProjection projection(String nickname, Long chatCount) {
        return new WeeklyChatRankRepository.WeeklyChatRankProjection() {
            @Override
            public String getNickname() {
                return nickname;
            }

            @Override
            public Long getChatCount() {
                return chatCount;
            }
        };
    }
}
