package org.nowstart.nyangnyangbot.controller;

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
import org.nowstart.nyangnyangbot.data.dto.favorite.FavoriteMeDto;
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.nowstart.nyangnyangbot.service.WeeklyChatRankService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerHistoryTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Mock
    private WeeklyChatRankService weeklyChatRankService;

    private FavoriteController favoriteController;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        favoriteController = new FavoriteController(favoriteService, authorizationRepository, weeklyChatRankService);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void favoriteHistory_ShouldExposeDateFieldExpectedByFrontend() throws Exception {
        FavoriteHistoryEntity history = FavoriteHistoryEntity.builder()
                .favorite(12)
                .history("출석체크(+1)")
                .build();
        ReflectionTestUtils.setField(history, "createDate", LocalDateTime.of(2026, 3, 22, 14, 30));
        given(favoriteService.getHistory("user1", 10)).willReturn(List.of(history));

        ResponseEntity<?> result = favoriteController.favoriteHistory("user1", 10);
        String json = objectMapper.writeValueAsString(result.getBody());

        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(json).contains("\"favorite\":12");
        then(json).contains("\"history\":\"출석체크(+1)\"");
        then(json).contains("\"date\":\"2026-03-22 14:30\"");
        then(json).doesNotContain("createDate");
    }

    @Test
    void favoriteHistory_ShouldExposeLedgerFields() throws Exception {
        FavoriteHistoryEntity history = FavoriteHistoryEntity.builder()
                .id(7L)
                .favorite(20)
                .history("출석체크(+5)")
                .delta(5)
                .balanceAfter(20)
                .sourceType(FavoriteSourceType.ATTENDANCE)
                .displayCategory("ATTENDANCE")
                .publicDescription("출석체크(+5)")
                .correctionOfLedgerId(null)
                .nickNameSnapshot("치즈냥")
                .build();
        ReflectionTestUtils.setField(history, "createDate", LocalDateTime.of(2026, 5, 9, 13, 20));
        given(favoriteService.getHistory("user1", 10)).willReturn(List.of(history));

        ResponseEntity<?> result = favoriteController.favoriteHistory("user1", 10);
        String json = objectMapper.writeValueAsString(result.getBody());

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
        given(favoriteService.getHistory("user1", 50)).willReturn(List.of());

        favoriteController.favoriteHistory("user1", 500);

        org.mockito.BDDMockito.then(favoriteService).should().getHistory("user1", 50);
    }

    @Test
    void favoriteMe_ShouldUseAuthenticatedUserId() {
        FavoriteMeDto dto = new FavoriteMeDto("user1", "치즈냥", 12, 2L, List.of());
        given(favoriteService.getMyFavorite("user1")).willReturn(dto);

        ResponseEntity<FavoriteMeDto> result = favoriteController.favoriteMe(
                new UsernamePasswordAuthenticationToken("user1", "N/A")
        );

        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo(dto);
        org.mockito.BDDMockito.then(favoriteService).should().getMyFavorite("user1");
    }
}
