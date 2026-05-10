package org.nowstart.nyangnyangbot.adapter.in.web;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.upbo.dto.UpboApplyDto;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.application.service.UpboService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AdminUpboControllerTest {

    @Mock
    private UpboService upboService;

    @Test
    void applyUpbo_ShouldPassAuthenticatedAdminAsActor() {
        AdminUpboController controller = new AdminUpboController(upboService);
        UpboApplyDto.Request request = new UpboApplyDto.Request(
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
        UpboApplyDto.Response response = new UpboApplyDto.Response(
                1L,
                "user-1",
                "칭찬 쿠폰",
                UpboStatus.OWNED,
                null,
                ConversionMode.NONE,
                null,
                "칭찬 쿠폰 지급"
        );
        given(upboService.applyUpbo(request, "admin-1")).willReturn(response);

        ResponseEntity<UpboApplyDto.Response> result = controller.applyUpbo(
                request,
                new UsernamePasswordAuthenticationToken("admin-1", "N/A")
        );

        then(result.getBody()).isEqualTo(response);
        BDDMockito.then(upboService).should().applyUpbo(request, "admin-1");
    }
}
