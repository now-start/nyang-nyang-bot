package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteHistoryResult;
import org.nowstart.nyangnyangbot.application.service.favorite.FavoriteService;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.ui.ExtendedModelMap;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerHistoryTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    private FavoriteController favoriteController;

    @BeforeEach
    void setUp() {
        favoriteController = new FavoriteController(favoriteService, weeklyChatRankService);
    }

    @Test
    @DisplayName("즐겨찾기 히스토리 fragment 모델에 화면이 기대하는 date 필드가 포함된다")
    void favoriteHistory_ShouldExposeDateFieldExpectedByFrontend() {
        // 준비
        FavoriteHistoryResult history = new FavoriteHistoryResult(
                null,
                null,
                null,
                null,
                12,
                null,
                null,
                "출석체크(+1)",
                false,
                12,
                "출석체크(+1)",
                LocalDateTime.of(2026, 3, 22, 14, 30)
        );
        given(favoriteService.getHistory("user1", 10)).willReturn(List.of(history));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = favoriteController.favoriteHistory("user1", 10, model);

        // 검증
        then(view).isEqualTo("features/favorite/components :: history-grid");
        FavoriteController.FavoriteHistoryView response = firstHistory(model);
        then(response.favorite()).isEqualTo(12);
        then(response.history()).isEqualTo("출석체크(+1)");
        then(response.date()).isEqualTo("2026-03-22 14:30");
    }

    @Test
    @DisplayName("즐겨찾기 히스토리 fragment 모델에 원장 필드들이 포함된다")
    void favoriteHistory_ShouldExposeLedgerFields() {
        // 준비
        FavoriteHistoryResult history = new FavoriteHistoryResult(
                7L,
                null,
                "치즈냥",
                5,
                20,
                FavoriteSourceType.ATTENDANCE.name(),
                "ATTENDANCE",
                "출석체크(+5)",
                false,
                20,
                "출석체크(+5)",
                LocalDateTime.of(2026, 5, 9, 13, 20)
        );
        given(favoriteService.getHistory("user1", 10)).willReturn(List.of(history));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        favoriteController.favoriteHistory("user1", 10, model);

        // 검증
        FavoriteController.FavoriteHistoryView response = firstHistory(model);
        then(response.ledgerId()).isEqualTo(7L);
        then(response.delta()).isEqualTo(5);
        then(response.balanceAfter()).isEqualTo(20);
        then(response.sourceType()).isEqualTo("ATTENDANCE");
        then(response.displayCategory()).isEqualTo("ATTENDANCE");
        then(response.nickNameSnapshot()).isEqualTo("치즈냥");
        then(response.correction()).isFalse();
    }

    @Test
    @DisplayName("limit이 50을 초과하면 50으로 제한한다")
    void favoriteHistory_ShouldClampLimitTo50() {
        // 준비
        given(favoriteService.getHistory("user1", 50)).willReturn(List.of());

        // 실행
        favoriteController.favoriteHistory("user1", 500, new ExtendedModelMap());

        // 검증
        org.mockito.BDDMockito.then(favoriteService).should().getHistory("user1", 50);
    }

    @Test
    @DisplayName("즐겨찾기 히스토리는 history-grid 조각을 반환한다")
    void favoriteHistory_ShouldReturnHistoryGridFragment() {
        // 준비
        FavoriteHistoryResult history = new FavoriteHistoryResult(
                7L,
                null,
                "치즈냥",
                5,
                20,
                FavoriteSourceType.ATTENDANCE.name(),
                "ATTENDANCE",
                "출석체크(+5)",
                false,
                20,
                "출석체크(+5)",
                LocalDateTime.of(2026, 5, 9, 13, 20)
        );
        given(favoriteService.getHistory("user1", 10)).willReturn(List.of(history));
        ExtendedModelMap model = new ExtendedModelMap();

        // 실행
        String view = favoriteController.favoriteHistory("user1", 10, model);

        // 검증
        then(view).isEqualTo("features/favorite/components :: history-grid");
        then((List<?>) model.get("histories")).hasSize(1);
    }

    @SuppressWarnings("unchecked")
    private FavoriteController.FavoriteHistoryView firstHistory(ExtendedModelMap model) {
        return ((List<FavoriteController.FavoriteHistoryView>) model.get("histories")).get(0);
    }
}
