package org.nowstart.nyangnyangbot.adapter.in.web.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase.OverlayDisplayResult;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.ui.ConcurrentModel;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.springframework.web.servlet.ModelAndView;

class OverlayControllerTest {

    @Test
    @DisplayName("룰렛 오버레이 요청 시 overlay-roulette 템플릿을 반환한다")
    void rouletteOverlay_ShouldReturnOverlayTemplate() {
        // 준비
        OverlayController controller = new OverlayController(BDDMockito.mock(ManageOverlayDisplayUseCase.class));

        // 실행
        ModelAndView result = controller.rouletteOverlay();

        // 검증
        then(result.getViewName()).isEqualTo("overlay-roulette");
        then(result.getModel()).isEmpty();
    }

    @Test
    @DisplayName("대기 이벤트가 없으면 오버레이 대기 fragment를 반환한다")
    void nextEvent_ShouldReturnWaitFragmentWhenQueueIsEmpty() {
        // 준비
        ManageOverlayDisplayUseCase useCase = BDDMockito.mock(ManageOverlayDisplayUseCase.class);
        given(useCase.claimNextEvent("Bearer token")).willReturn(Optional.empty());
        OverlayController controller = new OverlayController(useCase);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.nextEvent("Bearer token", model);

        // 검증
        then(view).isEqualTo("features/overlay/roulette :: overlay-wait");
        then(model.asMap()).isEmpty();
    }

    @Test
    @DisplayName("대기 이벤트가 있으면 이벤트 fragment를 반환하고 표시 완료 처리한다")
    void nextEvent_ShouldReturnEventFragmentAndMarkDisplayed() {
        // 준비
        ManageOverlayDisplayUseCase useCase = BDDMockito.mock(ManageOverlayDisplayUseCase.class);
        OverlayDisplayResult event = new OverlayDisplayResult(
                1L,
                20L,
                "치즈냥",
                1,
                5,
                LocalDateTime.of(2026, 5, 9, 12, 2),
                List.of()
        );
        given(useCase.claimNextEvent("Bearer token")).willReturn(Optional.of(event));
        OverlayController controller = new OverlayController(useCase);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.nextEvent("Bearer token", model);

        // 검증
        then(view).isEqualTo("features/overlay/roulette :: overlay-event");
        then(model.getAttribute("event")).isEqualTo(event);
        BDDMockito.then(useCase).should().markDisplayed(1L, "Bearer token");
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 오류 fragment를 반환한다")
    void nextEvent_ShouldReturnErrorFragmentForInvalidToken() {
        // 준비
        OverlayController controller = new OverlayController(BDDMockito.mock(ManageOverlayDisplayUseCase.class));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.nextEvent("", model);

        // 검증
        then(view).isEqualTo("features/overlay/roulette :: overlay-error");
        then(model.getAttribute("message")).isEqualTo("오버레이 토큰이 유효하지 않습니다.");
    }

    @Test
    @DisplayName("오버레이 템플릿은 초기 load와 대기 polling fragment를 분리한다")
    void overlayTemplate_ShouldSeparateInitialLoadAndPollingWait() {
        // 준비
        WebContext context = webContext();
        context.setVariable("token", "token");
        context.setVariable("message", "오버레이 오류");
        context.setVariable("event", new OverlayDisplayResult(
                1L,
                20L,
                "치즈냥",
                1,
                5,
                LocalDateTime.of(2026, 5, 9, 12, 2),
                List.of()
        ));

        // 실행
        String html = templateEngine().process("features/overlay/roulette", context);

        // 검증
        then(html).contains("hx-trigger=\"load\"");
        then(html).contains("hx-trigger=\"every 2s\"");
        then(html).contains("hx-trigger=\"load delay:5s\"");
        then(html).contains("hx-get=\"/overlay/roulette/events/next\"");
        then(html).contains("hx-headers");
        then(html).contains("Authorization");
        then(html).contains("URLSearchParams(location.hash.slice(1))");
        then(html).doesNotContain("token=");
        then(html).doesNotContain("hx-trigger=\"load, every 2s\"");
    }

    private WebContext webContext() {
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/overlay/roulette");
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
