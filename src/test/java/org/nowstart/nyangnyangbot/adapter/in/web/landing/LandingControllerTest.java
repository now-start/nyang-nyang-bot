package org.nowstart.nyangnyangbot.adapter.in.web.landing;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class LandingControllerTest {

    private final LandingController landingController = new LandingController();

    @Test
    @DisplayName("랜딩 페이지 요청 시 랜딩 모드가 활성화된 index 뷰를 반환한다")
    void landing_ShouldReturnIndexViewInLandingMode() {
        // 실행
        ModelAndView result = landingController.landing();

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(true);
    }
}
