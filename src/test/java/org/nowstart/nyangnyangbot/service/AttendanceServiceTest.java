package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.attendance.AttendanceDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteEntity;
import org.nowstart.nyangnyangbot.data.type.FavoriteHistoryType;
import org.nowstart.nyangnyangbot.repository.FavoriteHistoryRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private FavoriteAggregationService favoriteAggregationService;

    @Mock
    private FavoriteHistoryRepository favoriteHistoryRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void applyAttendance_ShouldRecalculateAndStoreAttendanceHistory() {
        FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                .userId("user1")
                .nickName("유저")
                .totalFavorite(20)
                .attendanceCount(2)
                .karmaScore(18)
                .build();
        given(favoriteAggregationService.getOrCreateFavorite("user1", "유저")).willReturn(favoriteEntity);
        given(favoriteAggregationService.recalculate(favoriteEntity))
                .willReturn(new FavoriteAggregationService.FavoriteTotals(18, 7, 25));

        AttendanceDto.ApplyResponse response = attendanceService.applyAttendance(
                new AttendanceDto.ApplyRequest(List.of(new AttendanceDto.User("user1", "유저", 123L)), 5)
        );

        then(response.amount()).isEqualTo(5);
        then(response.count()).isEqualTo(1);
        BDDMockito.then(favoriteAggregationService).should().getOrCreateFavorite("user1", "유저");
        BDDMockito.then(favoriteAggregationService).should().recalculate(favoriteEntity);
        BDDMockito.then(favoriteHistoryRepository).should().save(org.mockito.ArgumentMatchers.argThat(history ->
                history.getFavoriteEntity() == favoriteEntity
                        && Integer.valueOf(25).equals(history.getFavorite())
                        && Integer.valueOf(18).equals(history.getKarmaScore())
                        && Integer.valueOf(7).equals(history.getAttendanceCount())
                        && FavoriteHistoryType.ATTENDANCE.equals(history.getType())
                        && "출석체크(+5)".equals(history.getHistory())
        ));
    }

    @Test
    void applyAttendance_ShouldRejectNonPositiveAmount() {
        try {
            attendanceService.applyAttendance(
                    new AttendanceDto.ApplyRequest(List.of(new AttendanceDto.User("user1", "유저", 123L)), 0)
            );
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            then(ex).hasMessage("amount must be positive");
        }
    }
}
