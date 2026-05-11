package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.request.UpboApplyRequest;
import org.nowstart.nyangnyangbot.adapter.in.web.upbo.response.UpboApplyResponse;
import org.nowstart.nyangnyangbot.application.service.upbo.UpboService;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.domain.model.UserUpbo;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AdminUpboControllerTest {

    @Mock
    private UpboService upboService;

    @Test
    void applyUpbo_ShouldPassAuthenticatedAdminAsActor() {
        AdminUpboController controller = new AdminUpboController(upboService);
        UpboApplyRequest request = new UpboApplyRequest(
                "user-1",
                "치즈냥",
                null,
                "칭찬 쿠폰",
                RewardType.COUPON,
                ConversionMode.NONE,
                null,
                "칭찬 쿠폰 지급",
                "관리자 확인"
        );
        UserUpbo saved = new UserUpbo(
                1L,
                "user-1",
                null,
                "치즈냥",
                "칭찬 쿠폰",
                UpboStatus.OWNED,
                null,
                RewardType.COUPON,
                ConversionMode.NONE,
                FavoriteSourceType.UPBO_MANUAL,
                null,
                "칭찬 쿠폰 지급",
                "관리자 확인",
                "admin-1",
                null
        );
        given(upboService.applyUpbo(request.toCommand(), "admin-1")).willReturn(saved);

        ResponseEntity<UpboApplyResponse> result = controller.applyUpbo(
                request,
                new UsernamePasswordAuthenticationToken("admin-1", "N/A")
        );

        then(result.getBody()).isEqualTo(UpboApplyResponse.from(saved));
        BDDMockito.then(upboService).should().applyUpbo(request.toCommand(), "admin-1");
    }
}
