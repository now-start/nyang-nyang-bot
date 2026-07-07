package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteItemResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteTableResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteEventSummaryResult;
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

class RouletteTemplateTest {

    @Test
    void rouletteTemplate_ShouldRenderHtmxFragmentsWithData() {
        // 준비
        WebContext context = webContext();
        RouletteTableResult table = table();
        context.setVariable("tables", List.of(table));
        context.setVariable("selectedTableId", 1L);
        context.setVariable("table", table);
        context.setVariable("eventsPage", new PageImpl<>(
                List.of(event()),
                PageRequest.of(0, 5),
                12
        ));
        context.setVariable("tokenUrl", "https://example.com/overlay/roulette#token=raw-token");

        // 실행
        String html = templateEngine().process("features/roulette/components", context);

        // 검증
        then(html).contains("id=\"roulette-config-region\"");
        then(html).contains("룰렛 설정을 불러오는 중입니다.");
        then(html).contains("hx-get=\"/admin/roulette/tables/1/detail\"");
        then(html).contains("hx-target=\"#roulette-config-region\"");
        then(html).contains("hx-post=\"/admin/roulette/items\"");
        then(html).contains("id=\"roulette-selected-table-id\"");
        then(html).contains("name=\"tableId\"");
        then(html).contains("value=\"1\"");
        then(html).contains("id=\"roulette-detail\"");
        then(html).contains("당첨");
        then(html).contains("25.00%");
        then(html).contains("활성화 가능");
        then(html).containsSubsequence(
                "id=\"roulette-simulate\"",
                "hx-swap=\"outerHTML\"",
                "hx-target=\"#roulette-simulation\""
        );
        then(html).contains("hx-get=\"/admin/roulette/events?page=0&amp;size=5\"");
        then(html).contains("hx-trigger=\"roulette-event-refresh from:body\"");
        then(html).doesNotContain("hx-trigger=\"load, roulette-event-refresh");
        then(html).contains("hx-post=\"/admin/overlay/roulette/events/10/replay\"");
        then(html).contains("https://example.com/overlay/roulette#token=raw-token");
    }

    private RouletteTableResult table() {
        RouletteValidationResult validation = new RouletteValidationResult(true, List.of(), 10_000, true);
        RouletteItemResult item = new RouletteItemResult(
                1L,
                "당첨",
                2_500,
                false,
                "FAVORITE",
                "AUTO",
                100,
                true,
                1
        );
        return new RouletteTableResult(1L, "기본 룰렛", "!룰렛", 1_000L, true, 1, 100, validation, List.of(item));
    }

    private RouletteEventSummaryResult event() {
        return new RouletteEventSummaryResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                5,
                "APPLIED",
                LocalDateTime.of(2026, 6, 19, 15, 30)
        );
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
