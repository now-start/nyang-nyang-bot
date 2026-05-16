package org.nowstart.nyangnyangbot.application.service.upbo;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;

@ExtendWith(MockitoExtension.class)
class QueryUpboServiceTest {

    @Mock
    private UpboPort upboPort;

    @Test
    void getUserUpbos_ShouldLoadAllUserUpbos_WhenStatusIsBlank() {
        // 준비
        QueryUpboService service = new QueryUpboService(upboPort);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 16, 21, 40);
        given(upboPort.findUserUpbos("user-1")).willReturn(List.of(userUpbo(1L, UpboStatus.OWNED, createdAt)));

        // 실행
        List<UserUpboResult> result = service.getUserUpbos("user-1", " ");

        // 검증
        then(result).hasSize(1);
        then(result.getFirst().id()).isEqualTo(1L);
        then(result.getFirst().status()).isEqualTo("OWNED");
        then(result.getFirst().rewardType()).isEqualTo("FAVORITE");
        then(result.getFirst().conversionMode()).isEqualTo("AUTO");
        then(result.getFirst().createdAt()).isEqualTo(createdAt);
        BDDMockito.then(upboPort).should().findUserUpbos("user-1");
    }

    @Test
    void getUserUpbos_ShouldLoadByParsedStatus_WhenStatusIsProvided() {
        // 준비
        QueryUpboService service = new QueryUpboService(upboPort);
        given(upboPort.findUserUpbosByStatus("user-1", UpboStatus.CONVERTED))
                .willReturn(List.of(userUpbo(2L, null, LocalDateTime.of(2026, 5, 16, 21, 45))));

        // 실행
        List<UserUpboResult> result = service.getUserUpbos("user-1", " CONVERTED ");

        // 검증
        then(result).hasSize(1);
        then(result.getFirst().status()).isNull();
        BDDMockito.then(upboPort).should().findUserUpbosByStatus("user-1", UpboStatus.CONVERTED);
    }

    @Test
    void getUserUpbos_ShouldRejectBlankUserId() {
        // 준비
        QueryUpboService service = new QueryUpboService(upboPort);

        // 실행 및 검증
        thenThrownBy(() -> service.getUserUpbos("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
    }

    private UserResult userUpbo(Long id, UpboStatus status, LocalDateTime createdAt) {
        return new UserResult(
                id,
                "user-1",
                10L,
                "치즈냥",
                "호감도 +10",
                status,
                10,
                RewardType.FAVORITE,
                ConversionMode.AUTO,
                FavoriteSourceType.UPBO_ROULETTE,
                99L,
                "공개 설명",
                "내부 메모",
                "admin-1",
                createdAt
        );
    }
}
