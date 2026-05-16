package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.FavoriteMeResponse;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteHistoryResult;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteMeResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.application.service.favorite.FavoriteService;
import org.nowstart.nyangnyangbot.application.service.weeklychat.WeeklyChatRankService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerHistoryTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    private FavoriteController favoriteController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        favoriteController = new FavoriteController(favoriteService, weeklyChatRankService);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void favoriteHistory_ShouldExposeDateFieldExpectedByFrontend() throws Exception {
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

        // 실행
        ResponseEntity<?> result = favoriteController.favoriteHistory("user1", 10);
        String json = objectMapper.writeValueAsString(result.getBody());

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(json).contains("\"favorite\":12");
        then(json).contains("\"history\":\"출석체크(+1)\"");
        then(json).contains("\"date\":\"2026-03-22 14:30\"");
        then(json).doesNotContain("createDate");
    }

    @Test
    void favoriteHistory_ShouldExposeLedgerFields() throws Exception {
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

        // 실행
        ResponseEntity<?> result = favoriteController.favoriteHistory("user1", 10);
        String json = objectMapper.writeValueAsString(result.getBody());

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(json).contains("\"ledgerId\":7");
        then(json).contains("\"delta\":5");
        then(json).contains("\"balanceAfter\":20");
        then(json).contains("\"sourceType\":\"ATTENDANCE\"");
        then(json).contains("\"displayCategory\":\"ATTENDANCE\"");
        then(json).contains("\"nickNameSnapshot\":\"치즈냥\"");
        then(json).contains("\"correction\":false");
    }

    @Test
    void favoriteHistory_ShouldClampLimitTo50() {
        // 준비
        given(favoriteService.getHistory("user1", 50)).willReturn(List.of());

        // 실행
        favoriteController.favoriteHistory("user1", 500);

        // 검증
        org.mockito.BDDMockito.then(favoriteService).should().getHistory("user1", 50);
    }

    @Test
    void favoriteMe_ShouldUseAuthenticatedUserId() {
        // 준비
        FavoriteMeResult resultDto = new FavoriteMeResult("user1", "치즈냥", 12, 2L, List.of());
        given(favoriteService.getMyFavorite("user1")).willReturn(resultDto);

        // 실행
        ResponseEntity<FavoriteMeResponse> result = favoriteController.favoriteMe(
                new UsernamePasswordAuthenticationToken("user1", "N/A")
        );

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo(FavoriteMeResponse.from(resultDto));
        org.mockito.BDDMockito.then(favoriteService).should().getMyFavorite("user1");
    }
}
