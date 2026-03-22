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
import org.nowstart.nyangnyangbot.data.entity.FavoriteHistoryEntity;
import org.nowstart.nyangnyangbot.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.service.FavoriteService;
import org.nowstart.nyangnyangbot.service.WeeklyChatRankService;
import org.springframework.http.ResponseEntity;
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
}
