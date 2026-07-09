package org.nowstart.nyangnyangbot.adapter.in.web.root;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.ModelAndView;

class RootControllerTest {

    private final RootController rootController = new RootController();

    @Test
    @DisplayName("인증 정보가 없으면 랜딩 화면을 렌더링한다")
    void index_ShouldRenderLanding_WhenAuthenticationMissing() {
        // 실행
        ModelAndView result = rootController.index(null);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(true);
    }

    @Test
    @DisplayName("익명 인증이면 랜딩 화면을 렌더링한다")
    void index_ShouldRenderLanding_WhenAnonymousAuthentication() {
        // 준비
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        // 실행
        ModelAndView result = rootController.index(anonymous);

        // 검증
        then(result.getViewName()).isEqualTo("index");
        then(result.getModel().get("landingMode")).isEqualTo(true);
    }

    @Test
    @DisplayName("인증된 사용자는 호감도 목록으로 리다이렉트한다")
    void index_ShouldRedirectToFavoriteList_WhenAuthenticated() {
        // 준비
        UsernamePasswordAuthenticationToken authenticated =
                new UsernamePasswordAuthenticationToken("channel-1", "N/A", List.of());

        // 실행
        ModelAndView result = rootController.index(authenticated);

        // 검증
        then(result.getViewName()).isEqualTo("redirect:/favorite/list");
    }
}
