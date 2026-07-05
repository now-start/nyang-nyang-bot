package org.nowstart.nyangnyangbot.adapter.in.web.favorite;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.web.favorite.response.WeeklyChatRankResponse;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase.FavoriteSummaryResult;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
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
        context.setVariable("weeklyChatRanks", List.of(new WeeklyChatRankResponse(1, "치즈냥", 10L)));
        context.setVariable("favoriteList", new PageImpl<>(
                List.of(new FavoriteSummaryResult("user1", "유저1", 100)),
                PageRequest.of(0, 10),
                1
        ));

        // 실행
        String html = templateEngine.process("index", context);

        // 검증
        then(html).contains("명령어 관리");
        then(html).contains("id=\"command-tab\"");
        then(html).contains("id=\"command-list\"");
        then(html).contains("id=\"command-filter-type\"");
        then(html).contains("id=\"command-filter-active\"");
        then(html).contains("id=\"command-save\"");
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
