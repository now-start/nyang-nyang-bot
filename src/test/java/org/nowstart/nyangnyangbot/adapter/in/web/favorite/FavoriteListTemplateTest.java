package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.OptionView;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteSummaryResult;
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

class FavoriteListTemplateTest {

    @Test
    void indexTemplate_ShouldRenderAdminCommandPanel() {
        // 준비
        SpringTemplateEngine templateEngine = templateEngine();
        WebContext context = webContext();
        context.setVariable("landingMode", false);
        context.setVariable("isAdmin", true);
        context.setVariable("currentUserId", "admin");
        context.setVariable("currentNickName", "관리자");
        context.setVariable("nickName", "유저1");
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        setCommandOptions(context);
        context.setVariable("weeklyChatRanks", List.of(new FavoriteController.WeeklyChatRankView(1, "치즈냥", 10L)));
        context.setVariable("favoriteList", new PageImpl<>(
                List.of(new FavoriteSummaryResult("user1", "유저1", 100)),
                PageRequest.of(1, 10),
                25
        ));

        // 실행
        String html = templateEngine.process("index", context);

        // 검증
        then(html).contains("명령어 관리");
        then(html).containsSubsequence("호감도 목록", "출석 체크", "명령어 관리", "룰렛 관리", "오버레이 토큰");
        then(html).contains("BETA");
        then(html).contains("nav-beta-badge");
        then(html).contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css");
        then(html).contains("https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js");
        then(html).contains("https://unpkg.com/htmx.org@2.0.4");
        then(html).contains("bg-dark text-light");
        then(html).contains("hx-headers=");
        then(html).doesNotContain("favorite-list.css");
        then(html).doesNotContain("favorite-list.js");
        then(html).doesNotContain("design-system.css");
        then(html).contains("id=\"sync-button\"");
        then(html).contains("id=\"favorite-board-region\"");
        then(html).contains("hx-get=\"/favorite/list");
        then(html).contains("hx-trigger=\"favorite-board-refresh from:body\"");
        then(html).contains("hx-post=\"/google/sync\"");
        then(html).contains("hx-target=\"#sync-feedback\"");
        then(html).contains("id=\"chzzk-connect-button\"");
        then(html).contains("hx-post=\"/chzzk/connect\"");
        then(html).contains("hx-get=\"/favorite/history?userId=user1");
        then(html).contains("id=\"attendance-tab\"");
        then(html).contains("id=\"roulette-tab\"");
        then(html).contains("id=\"overlay-token-tab\"");
        then(html).contains("id=\"command-tab\"");
        then(html).contains("data-bs-toggle=\"tab\"");
        then(html).contains("data-bs-target=\"#favorite-tab\"");
        then(html).contains("class=\"tab-pane fade show active\"");
        then(html).contains("id=\"karma-modal\"");
        then(html).contains("class=\"modal fade\"");
        then(html).contains("data-bs-dismiss=\"modal\"");
        then(html).contains("hx-get=\"/favorite/adjustments/modal?userId=user1");
        then(html).contains("hx-target=\"#karma-modal-content\"");
        then(html).contains("data-bs-toggle=\"collapse\"");
        then(html).contains("id=\"command-list-region\"");
        then(html).contains("hx-get=\"/admin/commands\"");
        then(html).contains("id=\"command-editor-region\"");
        then(html).contains("id=\"command-filter-type\"");
        then(html).contains("id=\"command-filter-active\"");
        then(html).contains("id=\"attendance-amount\"");
        then(html).contains("hx-post=\"/attendance/apply\"");
        then(html).contains("hx-trigger=\"shown.bs.tab from:#attendance-tab-button\"");
        then(html).contains("hx-trigger=\"hidden.bs.tab from:#attendance-tab-button\"");
        then(html).contains("id=\"attendance-list\"");
        then(html).contains("attendance-users-refresh from:body");
        then(html).contains("id=\"roulette-config-region\"");
        then(html).contains("룰렛 설정을 불러오는 중입니다.");
        then(html).contains("hx-get=\"/admin/roulette/tables\"");
        then(html).contains("id=\"roulette-events-region\"");
        then(html).contains("최근 실행 기록을 불러오는 중입니다.");
        then(html).doesNotContain("hx-trigger=\"load, roulette-event-refresh");
        then(html).contains("id=\"overlay-token-url\"");
        then(html).contains("hx-post=\"/admin/overlay/roulette/token\"");
        then(html).contains("id=\"overlay-replay-form\"");
        then(html).contains("hx-post=\"/admin/overlay/roulette/events/replay\"");
        then(html).contains("form-select");
        then(html).contains("form-control");
        then(html).contains("btn btn-success");
        then(html).doesNotContain("jquery");
        then(html).doesNotContain("bootstrap/4.0.0");
        then(html).doesNotContain("command-modal");
        then(html).doesNotContain("btn-strong");
        then(html).doesNotContain("btn-ghost");
    }

    @Test
    void indexTemplate_ShouldHidePrivilegedControls_WhenNotAdmin() {
        // 준비
        SpringTemplateEngine templateEngine = templateEngine();
        WebContext context = webContext();
        context.setVariable("landingMode", false);
        context.setVariable("isAdmin", false);
        context.setVariable("currentUserId", "user1");
        context.setVariable("currentNickName", "유저1");
        context.setVariable("currentUserRank", 7);
        context.setVariable("nickName", "");
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        setCommandOptions(context);
        context.setVariable("weeklyChatRanks", List.of(new FavoriteController.WeeklyChatRankView(1, "치즈냥", 10L)));
        context.setVariable("userUpbos", List.of(new FavoriteController.UserUpboView(
                1L,
                "업보 차감권",
                "OWNED",
                null,
                "COUPON",
                "MANUAL",
                null,
                "업보 차감권 보유",
                "2026-07-09 19:30"
        )));
        context.setVariable("favoriteList", new PageImpl<>(
                List.of(new FavoriteSummaryResult("user1", "유저1", 100)),
                PageRequest.of(0, 10),
                1
        ));

        // 실행
        String html = templateEngine.process("index", context);

        // 검증
        then(html).contains("id=\"favorite-board-region\"");
        then(html).contains("내 계정의 호감도, 실제 순위, 보유 업보만 표시합니다.");
        then(html).contains("전체 순위");
        then(html).contains("7위");
        then(html).contains("보유 업보 · 쿠폰 · 리워드");
        then(html).contains("업보 차감권");
        then(html).contains("유저1");
        then(html).doesNotContain("유저2");
        then(html).contains("주간 채팅 순위 TOP 10");
        then(html).contains("hx-get=\"/favorite/history?userId=user1");
        then(html).doesNotContain("hx-get=\"/favorite/history?userId=user2");
        then(html).doesNotContain("관리자 메뉴");
        then(html).doesNotContain("id=\"favorite-search-form\"");
        then(html).doesNotContain("id=\"sync-button\"");
        then(html).doesNotContain("id=\"chzzk-connect-button\"");
        then(html).doesNotContain("id=\"attendance-tab\"");
        then(html).doesNotContain("id=\"roulette-tab\"");
        then(html).doesNotContain("id=\"overlay-token-tab\"");
        then(html).doesNotContain("id=\"command-tab\"");
        then(html).doesNotContain("id=\"karma-modal\"");
        then(html).doesNotContain("호감도 수정");
        then(html).doesNotContain("hx-get=\"/favorite/adjustments/modal");
        then(html).doesNotContain("hx-post=\"/favorite/adjustments/apply");
        then(html).doesNotContain("hx-post=\"/google/sync");
        then(html).doesNotContain("hx-post=\"/chzzk/connect");
        then(html).doesNotContain("hx-post=\"/attendance/");
        then(html).doesNotContain("hx-get=\"/admin/");
        then(html).doesNotContain("hx-post=\"/admin/");
        then(html).doesNotContain("data-is-admin");
        then(html).doesNotContain("data-current-user-id");
    }

    @Test
    void indexTemplate_ShouldRenderNegativeBalanceWithTripleIndicator() {
        // 준비
        SpringTemplateEngine templateEngine = templateEngine();
        WebContext context = webContext();
        context.setVariable("landingMode", false);
        context.setVariable("isAdmin", false);
        context.setVariable("currentUserId", "user1");
        context.setVariable("currentNickName", "유저1");
        context.setVariable("nickName", "");
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        setCommandOptions(context);
        context.setVariable("weeklyChatRanks", List.of(new FavoriteController.WeeklyChatRankView(1, "치즈냥", 10L)));
        context.setVariable("favoriteList", new PageImpl<>(
                List.of(
                        new FavoriteSummaryResult("user1", "유저1", -12),
                        new FavoriteSummaryResult("user2", "유저2", null)
                ),
                PageRequest.of(0, 10),
                2
        ));

        // 실행
        String html = templateEngine.process("index", context);

        // 검증 — 음수 잔액은 색 + 배지 + 숫자 3중 표시
        then(html).contains("bg-danger-subtle");
        then(html).contains("음수 잔액");
        then(html).contains("text-danger");
        then(html).contains("-12");
        // null favorite는 0으로 정규화되어 렌더링된다
        then(html).contains("유저2");
    }

    @Test
    void indexTemplate_ShouldRenderLandingHero_WhenLandingMode() {
        // 준비
        SpringTemplateEngine templateEngine = templateEngine();
        WebContext context = webContext();
        context.setVariable("landingMode", true);

        // 실행
        String html = templateEngine.process("index", context);

        // 검증 — 2a 랜딩: 로고 + h1 + 설명 + 로그인 버튼 + 보안 각주 + 기능 도트 리스트
        then(html).contains("bg-landing-glow");
        then(html).contains("치즈냥 호감도");
        then(html).contains("치지직 계정으로 로그인");
        then(html).contains("href=\"/oauth2/authorization/chzzk\"");
        then(html).contains("CHZZK OAuth로 안전하게 로그인합니다");
        then(html).contains("호감도 랭킹");
        then(html).contains("/css/theme.css");
        then(html).doesNotContain("http-equiv=\"refresh\"");
        then(html).doesNotContain("id=\"favorite-board-region\"");
        then(html).doesNotContain("관리자 메뉴");
    }

    @Test
    void karmaModalContentTemplate_ShouldRenderApplyForm() {
        // 준비
        SpringTemplateEngine templateEngine = templateEngine();
        WebContext context = webContext();
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token"));
        context.setVariable("target", new FavoriteAdjustmentController.FavoriteAdjustmentTarget("user1", "유저1", 100));
        context.setVariable("adjustments", List.of(new FavoriteAdjustmentController.FavoriteAdjustmentOptionView(1L, 5, "출석 보너스")));

        // 실행
        String html = templateEngine.process("features/favorite/overlays", context);

        // 검증
        then(html).contains("id=\"karma-form\"");
        then(html).contains("hx-post=\"/favorite/adjustments/apply\"");
        then(html).contains("name=\"userId\"");
        then(html).contains("value=\"user1\"");
        then(html).contains("name=\"adjustmentIds\"");
        then(html).contains("value=\"1\"");
        then(html).contains("id=\"manual-amount\"");
        then(html).contains("id=\"manual-history\"");
        then(html).contains("value=\"test-token\"");
        then(html).contains("btn btn-success");
    }

    private void setCommandOptions(WebContext context) {
        context.setVariable("commandTypeOptions", List.of(
                new OptionView("", "전체 유형"),
                new OptionView("TEXT", "TEXT"),
                new OptionView("TRIGGER", "TRIGGER"),
                new OptionView("TIMER", "TIMER")
        ));
        context.setVariable("commandEditorTypeOptions", List.of(
                new OptionView("TEXT", "TEXT"),
                new OptionView("TRIGGER", "TRIGGER"),
                new OptionView("TIMER", "TIMER")
        ));
        context.setVariable("commandActiveOptions", List.of(
                new OptionView("", "전체 상태"),
                new OptionView("true", "활성"),
                new OptionView("false", "비활성")
        ));
        context.setVariable("commandActionOptions", List.of(
                new OptionView("", "없음"),
                new OptionView("FAVORITE_STATUS", "FAVORITE_STATUS"),
                new OptionView("ROULETTE_RESULT", "ROULETTE_RESULT"),
                new OptionView("ROULETTE_DONATION", "ROULETTE_DONATION")
        ));
        context.setVariable("commandRoleOptions", List.of(
                new OptionView("USER", "USER"),
                new OptionView("ADMIN", "ADMIN")
        ));
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/favorite/list");
        request.setContextPath("");
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
