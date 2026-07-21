package org.nowstart.nyangnyangbot.adapter.in.web.command;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.CommandForm;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.CommandView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.OptionView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.ReviewView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.VariableGroupView;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.VariableResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class CommandTemplateTest {

    @Test
    void commandTemplate_ShouldRenderComponentizedRegions() {
        // 준비
        WebContext context = webContext();
        setCommandOptions(context);
        context.setVariable("commands", List.of(CommandView.from(command())));
        context.setVariable("commandForm", CommandForm.from(command()));
        context.setVariable("saveMessage", "저장됨");
        context.setVariable("reviewResult", new ReviewView(true, List.of(), "치즈냥님, 오늘 공지는 ..."));

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
        then(html).contains("type=\"hidden\"");
        then(html).contains("id=\"command-filter-active\"");
        then(html).contains("id=\"command-trigger\"");
        then(html).contains("id=\"command-message-template\"");
        then(html).contains("maxlength=\"1000\"");
        then(html).contains("hx-post=\"/admin/commands\"");
        then(html).contains("hx-post=\"/admin/commands/preview\"");
        then(html).contains("hx-include=\"#command-editor-form [name=commandId]\"");
        then(html).doesNotContain("hx-post=\"/admin/commands/validate\"");
        then(html).contains("검증 및 미리보기");
        then(html).contains("변경사항 저장");
        then(html).contains("비활성화");
        then(html).contains("변수 넣기");
        then(html).contains("data-bs-toggle=\"dropdown\"");
        then(html).contains("data-template-target=\"#command-message-template\"");
        then(html).contains("data-template-variable=\"{viewer.nickname}\"");
        then(html).contains("시청자");
        then(html).contains("연동 데이터");
        then(html).contains("{viewer.nickname}");
        then(html).contains("시청자 닉네임");
        then(html).contains("예: 치즈냥");
        then(html).contains("냥냥봇");
        then(html).contains("치즈냥님, 오늘 공지는 ...");
        then(html).contains("data-review-target=\"#command-review-region\"");
        then(html).contains("class=\"template-preview-message\"");
        then(html).contains("role=\"status\"");
        then(html).contains("form-select");
        then(html).contains("form-control");
        then(html).doesNotContain("id=\"command-filter-type\"");
        then(html).doesNotContain("name=\"type\"");
        then(html).doesNotContain("name=\"actionKey\"");
        then(html).doesNotContain("name=\"timerIntervalMinutes\"");
        then(html).doesNotContain("name=\"timerMinChatCount\"");
        then(html).doesNotContain("name=\"requiredRole\"");
        then(html).doesNotContain("id=\"command-preview-nickname\"");
        then(html).doesNotContain("id=\"command-preview-args\"");
        then(html).doesNotContain("id=\"command-preview-favorite\"");
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
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/commands\"");
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/commands/editor\"");
        then(html).contains("hx-get=\"/nyang-nyang-bot/admin/commands/editor/new\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/commands\"");
        then(html).contains("hx-post=\"/nyang-nyang-bot/admin/commands/preview\"");
        then(html).doesNotContain("hx-post=\"/nyang-nyang-bot/admin/commands/validate\"");
    }

    @Test
    void commandEditor_ShouldRenderInitialPlaceholderWithoutForm() {
        // 준비
        WebContext context = webContext();
        context.setVariable("saveError", "명령어를 찾을 수 없습니다.");

        // 실행
        String html = templateEngine().process("features/command/regions", context);

        // 검증
        then(html).contains("명령어를 선택하거나 새 명령어를 만드세요.");
        then(html).contains("role=\"alert\"");
        then(html).contains("명령어를 찾을 수 없습니다.");
        then(html).doesNotContain("id=\"command-editor-form\"");
        then(html).doesNotContain("명령어 생성");
        then(html).doesNotContain("변경사항 저장");
    }

    @Test
    void commandEditor_ShouldRenderCreateStateWithoutDeactivateAction() {
        // 준비
        WebContext context = webContext();
        setCommandOptions(context);
        context.setVariable("commandForm", CommandForm.empty());

        // 실행
        String html = templateEngine().process("features/command/regions", context);

        // 검증
        then(html).contains("새 명령어");
        then(html).contains("명령어 생성");
        then(html).contains("placeholder=\"!호감도 또는 치하\"");
        then(html).doesNotContain("비활성화");
    }

    static void setCommandOptions(WebContext context) {
        context.setVariable("commandActiveOptions", List.of(
                new OptionView("", "전체 상태"),
                new OptionView("true", "활성"),
                new OptionView("false", "비활성")
        ));
        context.setVariable("commandVariableGroups", List.of(
                new VariableGroupView("viewer", "시청자", List.of(
                        new VariableResult(
                                "viewer.nickname",
                                "시청자 닉네임",
                                "명령어를 호출한 시청자의 닉네임",
                                "치즈냥"
                        )
                )),
                new VariableGroupView("integration", "연동 데이터", List.of(
                        new VariableResult(
                                "favorite.balance",
                                "호감도",
                                "명령어를 호출한 시청자의 현재 호감도",
                                "100"
                        )
                ))
        ));
    }

    private CommandResult command() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 6, 12, 0);
        return new CommandResult(
                1L,
                "!공지",
                "{viewer.nickname}님",
                true,
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
