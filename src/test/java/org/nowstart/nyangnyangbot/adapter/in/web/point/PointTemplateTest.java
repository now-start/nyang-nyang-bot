package org.nowstart.nyangnyangbot.adapter.in.web.point;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase.PointSummaryResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class PointTemplateTest {

    @Test
    void indexTemplate_ShouldRenderCanonicalAdminEndpoints() {
        WebContext context = webContext();
        setBaseBoardModel(context, true, 100L);
        context.setVariable("commandActiveOptions", List.of());
        context.setVariable("commandVariables", List.of());

        String html = templateEngine().process("index", context);

        then(html).contains("id=\"point-board-region\"");
        then(html).contains("hx-get=\"/points/list");
        then(html).contains("hx-get=\"/points/history?userId=user1");
        then(html).contains("hx-get=\"/points/adjustments/modal?userId=user1");
        then(html).contains("id=\"point-adjustment-modal\"");
        then(html).contains("id=\"presence-tab\"");
        then(html).contains("hx-post=\"/presence-rewards/apply\"");
        then(html).contains("hx-post=\"/presence-rewards/start\"");
        then(html).contains("hx-post=\"/presence-rewards/stop\"");
        then(html).doesNotContain("/favorite/");
        then(html).doesNotContain("/attendance/");
        then(html).doesNotContain("favorite-board-region");
    }

    @Test
    void indexTemplate_ShouldRenderOwnNegativePointAndRewardWithoutAdminControls() {
        WebContext context = webContext();
        setBaseBoardModel(context, false, -12L);
        context.setVariable("currentUserRank", 7L);
        context.setVariable("userRewards", List.of(new PointController.RewardView(
                1L,
                "업보 차감권",
                "OWNED",
                null,
                "COUPON",
                "MANUAL",
                null,
                "보유 보상",
                "2026-07-09 19:30"
        )));

        String html = templateEngine().process("index", context);

        then(html).contains("id=\"point-board-region\"");
        then(html).contains("7위");
        then(html).contains("음수 잔액");
        then(html).contains("text-danger");
        then(html).contains("-12");
        then(html).contains("업보 차감권");
        then(html).contains("hx-get=\"/points/history?userId=user1");
        then(html).doesNotContain("관리자 메뉴");
        then(html).doesNotContain("id=\"point-search-form\"");
        then(html).doesNotContain("id=\"presence-tab\"");
        then(html).doesNotContain("id=\"point-adjustment-modal\"");
    }

    @Test
    void pointAdjustmentTemplate_ShouldRenderCanonicalApplyForm() {
        WebContext context = webContext();
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        context.setVariable(
                "target",
                new PointAdjustmentController.PointAdjustmentTarget("user1", "유저1", 100L)
        );
        context.setVariable(
                "presets",
                List.of(new PointAdjustmentController.PointAdjustmentPresetView(1L, 5L, "생존 보너스"))
        );

        String html = templateEngine().process("features/point/overlays", context);

        then(html).contains("id=\"point-adjustment-form\"");
        then(html).contains("hx-post=\"/points/adjustments/apply\"");
        then(html).contains("name=\"userId\"");
        then(html).contains("value=\"user1\"");
        then(html).contains("name=\"presetIds\"");
        then(html).contains("value=\"1\"");
        then(html).contains("id=\"manual-amount\"");
        then(html).contains("id=\"manual-description\"");
        then(html).contains("value=\"test-token\"");
    }

    private void setBaseBoardModel(WebContext context, boolean admin, long point) {
        context.setVariable("landingMode", false);
        context.setVariable("isAdmin", admin);
        context.setVariable("currentUserId", admin ? "admin" : "user1");
        context.setVariable("currentNickName", admin ? "관리자" : "유저1");
        context.setVariable("currentUserRank", null);
        context.setVariable("nickName", admin ? "유저1" : "");
        context.setVariable("localTestLoginEnabled", false);
        context.setVariable("userRewards", List.of());
        context.setVariable("weeklyChatRanks", List.of(new PointController.WeeklyChatRankView(1, "치즈냥", 10L)));
        context.setVariable("pointList", new PageImpl<>(
                List.of(new PointSummaryResult("user1", "유저1", point)),
                PageRequest.of(0, 10),
                1
        ));
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/points/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        return new WebContext(
                JakartaServletWebApplication.buildApplication(servletContext).buildExchange(request, response),
                Locale.KOREAN
        );
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        return templateEngine;
    }
}
