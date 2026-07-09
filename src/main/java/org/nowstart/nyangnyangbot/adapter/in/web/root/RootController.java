package org.nowstart.nyangnyangbot.adapter.in.web.root;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Tag(name = "Root", description = "기본 진입 페이지")
public class RootController {

    private static final String LANDING_VIEW = "index";
    private static final String FAVORITE_LIST_REDIRECT = "redirect:/favorite/list";

    @Operation(summary = "기본 페이지", description = "미인증 사용자는 랜딩 화면을, 인증 사용자는 호감도 보드로 이동합니다.")
    @GetMapping({"", "/"})
    public ModelAndView index(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return new ModelAndView(FAVORITE_LIST_REDIRECT);
        }
        return new ModelAndView(LANDING_VIEW, "landingMode", true);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
