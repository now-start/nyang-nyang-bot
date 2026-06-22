package org.nowstart.nyangnyangbot.adapter.in.web.root;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RootControllerTest {

    private final RootController rootController = new RootController();

    @Test
    @DisplayName("기본 페이지 요청 시 호감도 목록으로 리다이렉트한다")
    void index_ShouldRedirectToFavoriteList() {
        // 실행
        String result = rootController.index();

        // 검증
        then(result).isEqualTo("redirect:/favorite/list");
    }
}
