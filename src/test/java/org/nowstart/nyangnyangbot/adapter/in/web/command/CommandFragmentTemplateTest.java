package org.nowstart.nyangnyangbot.adapter.in.web.command;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandFragmentController.CommandForm;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandFragmentController.CommandView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandFragmentController.OptionView;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class CommandFragmentTemplateTest {

    @Test
    void commandTemplate_ShouldRenderComponentizedRegions() {
        // 준비
        WebContext context = webContext();
        setCommandOptions(context);
        context.setVariable("commands", List.of(CommandView.from(command())));
        context.setVariable("commandForm", CommandForm.from(command()));
        context.setVariable("saveMessage", "저장됨");

        // 실행
        String html = templateEngine().process("features/command/regions", context);

        // 검증
        then(html).contains("id=\"command-filter-form\"");
        then(html).contains("id=\"command-list-region\"");
        then(html).contains("id=\"command-editor-region\"");
        then(html).contains("table table-dark table-hover");
        then(html).contains("쿨타임");
        then(html).contains("수정자");
        then(html).contains("30초");
        then(html).contains("admin");
        then(html).contains("hx-trigger=\"command-list-refresh from:body\"");
        then(html).contains("name=\"type\"");
        then(html).contains("value=\"TEXT\"");
        then(html).contains("type=\"hidden\"");
        then(html).contains("id=\"command-type\"");
        then(html).contains("disabled=\"disabled\"");
        then(html).contains("hx-post=\"/admin/commands/fragments/save\"");
        then(html).contains("hx-post=\"/admin/commands/fragments/validate\"");
        then(html).contains("hx-post=\"/admin/commands/fragments/preview\"");
        then(html).contains("form-select");
        then(html).contains("form-control");
        then(html).doesNotContain("components/common");
        then(html).doesNotContain("command-modal");
    }

    @Test
    void commandTemplate_ShouldRenderContextPathAwareHtmxUrls() {
        // 준비
        WebContext context = webContext("/nyang-nyang-bot");
        setCommandOptions(context);
        context.setVariable("commands", List.of(CommandView.from(command())));
        context.setVariable("commandForm", CommandForm.from(command()));

        // 실행
        String html = templateEngine().process("features/command/regions", context);

        // 검증
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/commands/fragments/list\"");
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/commands/fragments/editor\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/commands/fragments/save\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/commands/fragments/validate\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/commands/fragments/preview\"");
    }

    static void setCommandOptions(WebContext context) {
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

    private CommandResult command() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 12, 0);
        return new CommandResult(
                1L,
                "TEXT",
                "!공지",
                null,
                "{nickname}님",
                10,
                10,
                true,
                "USER",
                30,
                "admin",
                "admin",
                now,
                now
        );
    }

    private WebContext webContext() {
        return webContext("");
    }

    private WebContext webContext(String contextPath) {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/favorite/list");
        request.setContextPath(contextPath);
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
