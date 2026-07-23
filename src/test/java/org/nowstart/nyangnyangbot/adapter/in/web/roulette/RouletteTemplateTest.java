package org.nowstart.nyangnyangbot.adapter.in.web.roulette;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteConfigResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteOptionResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ManageRouletteUseCase.RouletteValidationResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRunSummaryResult;
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

    private static final Instant CREATED_AT = Instant.parse("2026-06-19T06:00:00Z");

    @Test
    void rouletteTemplate_ShouldRenderHtmxFragmentsWithCanonicalModels() {
        WebContext context = webContext();
        RouletteConfigResult config = validConfig();
        context.setVariable("configs", List.of(config));
        context.setVariable("selectedConfigId", 1L);
        context.setVariable("config", config);
        context.setVariable("runsPage", new PageImpl<>(
                List.of(run()),
                PageRequest.of(0, 5),
                12
        ));
        context.setVariable("tokenUrl", "https://example.com/overlay/roulette#token=raw-token");

        String html = templateEngine().process("features/roulette/components", context);

        then(html).contains("id=\"roulette-config-region\"");
        then(html).contains("룰렛 설정을 불러오는 중입니다.");
        then(html).contains("hx-get=\"/admin/roulette/configs/1/detail\"");
        then(html).contains("hx-target=\"#roulette-config-region\"");
        then(html).contains("hx-post=\"/admin/roulette/options\"");
        then(html).contains("name=\"configId\"");
        then(html).contains("value=\"1\"");
        then(html).contains("id=\"roulette-option-list\"");
        then(html).contains("당첨");
        then(html).contains("25.00%");
        then(html).contains("검증 통과");
        then(html).contains("hx-get=\"/admin/roulette/configs/1/simulation?iterations=10000\"");
        then(html).contains("hx-target=\"#roulette-simulation\"");
        then(html).contains("hx-get=\"/admin/roulette/runs\"");
        then(html).contains("hx-trigger=\"roulette-run-refresh from:body\"");
        then(html).doesNotContain("roulette-event-refresh");
        then(html).contains("hx-post=\"/admin/overlay/roulette/runs/10/replay\"");
        then(html).contains("https://example.com/overlay/roulette#token=raw-token");
        then(html).contains("POINT");
    }

    @Test
    void rouletteTemplate_ShouldDisableActivation_WhenValidationFails() {
        WebContext context = webContext();
        RouletteValidationResult invalidValidation = new RouletteValidationResult(
                false,
                List.of("probability total must be 10000"),
                8_000,
                false
        );
        RouletteOptionResult option = new RouletteOptionResult(
                1L, "당첨", 8_000, false, "POINT", "AUTO", 100L, 1
        );
        RouletteConfigResult config = new RouletteConfigResult(
                1L,
                "기본 룰렛",
                "!룰렛",
                1_000L,
                "DRAFT",
                100,
                invalidValidation,
                List.of(option),
                CREATED_AT,
                CREATED_AT
        );
        context.setVariable("configs", List.of(config));
        context.setVariable("selectedConfigId", 1L);
        context.setVariable("config", config);
        context.setVariable("runsPage", null);
        context.setVariable("tokenUrl", null);

        String html = templateEngine().process("features/roulette/components", context);

        then(html).contains("alert-danger");
        then(html).contains("probability total must be 10000");
        then(html).doesNotContain("검증 통과");
        then(html).contains("hx-post=\"/admin/roulette/configs/1/activate\"");
        then(html).contains("disabled=\"disabled\"");
    }

    private RouletteConfigResult validConfig() {
        RouletteValidationResult validation = new RouletteValidationResult(true, List.of(), 10_000, true);
        RouletteOptionResult winner = new RouletteOptionResult(
                1L,
                "당첨",
                2_500,
                false,
                "POINT",
                "AUTO",
                100L,
                1
        );
        RouletteOptionResult losing = new RouletteOptionResult(
                2L,
                "꽝",
                7_500,
                true,
                "CUSTOM",
                "NONE",
                null,
                2
        );
        return new RouletteConfigResult(
                1L,
                "기본 룰렛",
                "!룰렛",
                1_000L,
                "DRAFT",
                100,
                validation,
                List.of(winner, losing),
                CREATED_AT,
                CREATED_AT
        );
    }

    private RouletteRunSummaryResult run() {
        return new RouletteRunSummaryResult(
                10L,
                "donation-1",
                "user-1",
                "치즈냥",
                5_000L,
                5,
                "APPLIED",
                Instant.parse("2026-06-19T06:30:00Z")
        );
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/point/list");
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
