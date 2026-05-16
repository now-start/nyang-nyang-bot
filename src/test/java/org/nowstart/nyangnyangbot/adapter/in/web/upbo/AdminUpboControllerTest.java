package org.nowstart.nyangnyangbot.adapter.in.web.upbo;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.request.UpboApplyRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.response.UpboApplyResponse;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase;
import org.nowstart.nyangnyangbot.application.port.in.upbo.ManageUpboUseCase.UserUpboResult;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AdminUpboControllerTest {

    @Mock
    private ManageUpboUseCase upboService;

    @Test
    void applyUpbo_ShouldPassAuthenticatedAdminAsActor() {
        // 준비
        AdminUpboController controller = new AdminUpboController(upboService);
        UpboApplyRequest request = new UpboApplyRequest(
                "user-1",
                "치즈냥",
                null,
                "칭찬 쿠폰",
                RewardType.COUPON.name(),
                ConversionMode.NONE.name(),
                null,
                "칭찬 쿠폰 지급",
                "관리자 확인"
        );
        UserUpboResult saved = new UserUpboResult(
                1L,
                "user-1",
                "치즈냥",
                "칭찬 쿠폰",
                UpboStatus.OWNED.name(),
                null,
                RewardType.COUPON.name(),
                ConversionMode.NONE.name(),
                null,
                "칭찬 쿠폰 지급",
                null
        );
        given(upboService.applyUpbo(request.toApplyUpboCommand(), "admin-1")).willReturn(saved);

        // 실행
        ResponseEntity<UpboApplyResponse> result = controller.applyUpbo(
                request,
                new UsernamePasswordAuthenticationToken("admin-1", "N/A")
        );

        // 검증
        then(result.getBody()).isEqualTo(UpboApplyResponse.from(saved));
        BDDMockito.then(upboService).should().applyUpbo(request.toApplyUpboCommand(), "admin-1");
    }
}
