package org.nowstart.nyangnyangbot.adapter.in.web.timer;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.timer.TimerMessageController.TimerMessageForm;
import org.nowstart.nyangnyangbot.adapter.in.web.timer.TimerMessageController.TimerMessageView;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.CreateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.PreviewResult;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.TimerMessageResult;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.UpdateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.ValidationResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class TimerMessageControllerTest {

    @Mock
    private ManageTimerMessageUseCase manageTimerMessageUseCase;

    @InjectMocks
    private TimerMessageController controller;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @Test
    void list_ShouldReturnFilteredTimerListFragment() {
        given(manageTimerMessageUseCase.getTimerMessages()).willReturn(List.of(
                timer(1L, true),
                timer(2L, false)
        ));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.list(true, model);

        then(view).isEqualTo("features/timer/regions :: timer-list-region");
        @SuppressWarnings("unchecked")
        List<TimerMessageView> timers = (List<TimerMessageView>) model.getAttribute("timerMessages");
        then(timers).hasSize(1);
        then(timers.getFirst().intervalLabel()).isEqualTo("30분");
        then(timers.getFirst().chatProgressLabel()).isEqualTo("7 / 10");
        then(timers.getFirst().nextRunLabel()).isEqualTo("2026-07-16 21:30");
    }

    @Test
    void editor_WithoutId_ShouldRenderInitialPlaceholder() {
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.editor(null, model);

        then(view).isEqualTo("features/timer/regions :: timer-editor-region");
        then(model.containsAttribute("timerMessageForm")).isFalse();
    }

    @Test
    void editor_WhenTimerDoesNotExist_ShouldRenderErrorInsteadOfThrowing() {
        given(manageTimerMessageUseCase.getTimerMessages()).willReturn(List.of());
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.editor(999L, model);

        then(view).isEqualTo("features/timer/regions :: timer-editor-region");
        then(model.getAttribute("saveError")).isEqualTo("타이머를 찾을 수 없습니다.");
        then(model.containsAttribute("timerMessageForm")).isFalse();
    }

    @Test
    void newEditor_ShouldRenderCreationFormWithTimeVariables() {
        given(manageTimerMessageUseCase.getVariables()).willReturn(List.of(
                new ManageTimerMessageUseCase.VariableResult(
                        "time.datetime", "현재 일시", "실행 시점의 날짜와 시각", "2026-07-16 21:00")
        ));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.newEditor(model);

        then(view).isEqualTo("features/timer/regions :: timer-editor-region");
        TimerMessageForm form = (TimerMessageForm) model.getAttribute("timerMessageForm");
        then(form.timerMessageId()).isNull();
        then(form.intervalMinutes()).isEqualTo(30);
        then(form.minChatCount()).isEqualTo(10);
        then(model.getAttribute("timerVariables")).isNotNull();
    }

    @Test
    void preview_WhenInvalid_ShouldNotRenderPreview() {
        given(manageTimerMessageUseCase.validate(any()))
                .willReturn(new ValidationResult(false, List.of("messageTemplate is required")));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.preview(TimerMessageForm.empty(), model);

        then(view).isEqualTo("features/timer/regions :: timer-review-region");
        TimerMessageController.ReviewView review =
                (TimerMessageController.ReviewView) model.getAttribute("timerReview");
        then(review.valid()).isFalse();
        then(review.errors()).containsExactly("messageTemplate is required");
        org.mockito.BDDMockito.then(manageTimerMessageUseCase)
                .should(org.mockito.Mockito.never())
                .preview(any());
    }

    @Test
    void preview_WhenValid_ShouldReturnValidationAndRenderedMessageTogether() {
        given(manageTimerMessageUseCase.validate(any()))
                .willReturn(new ValidationResult(true, List.of()));
        given(manageTimerMessageUseCase.preview(any()))
                .willReturn(new PreviewResult("현재 시각은 21:00입니다."));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.preview(new TimerMessageForm(
                null, "현재 시각은 {time.time}입니다.", 30, 10, true, 0L, null, null
        ), model);

        then(view).isEqualTo("features/timer/regions :: timer-review-region");
        TimerMessageController.ReviewView review =
                (TimerMessageController.ReviewView) model.getAttribute("timerReview");
        then(review.valid()).isTrue();
        then(review.previewMessage()).isEqualTo("현재 시각은 21:00입니다.");
    }

    @Test
    void save_ShouldCreateTimerAndRefreshList() {
        given(manageTimerMessageUseCase.createTimerMessage(any(CreateTimerMessage.class)))
                .willReturn(timer(1L, false));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.save(TimerMessageForm.empty(), false, null, response, model);

        then(view).isEqualTo("features/timer/regions :: timer-editor-region");
        then(model.getAttribute("saveMessage")).isEqualTo("저장됨");
        then(response.getHeader("HX-Trigger")).isEqualTo("timer-list-refresh");
        ArgumentCaptor<CreateTimerMessage> captor = ArgumentCaptor.forClass(CreateTimerMessage.class);
        org.mockito.BDDMockito.then(manageTimerMessageUseCase).should().createTimerMessage(captor.capture());
        then(captor.getValue().active()).isFalse();
    }

    @Test
    void deactivate_ShouldRequestOnlyActiveStateChange() {
        TimerMessageResult inactive = timer(1L, false);
        given(manageTimerMessageUseCase.updateTimerMessage(any(), any(UpdateTimerMessage.class)))
                .willReturn(inactive);
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.deactivate(1L, null, response, model);

        then(view).isEqualTo("features/timer/regions :: timer-editor-region");
        then(((TimerMessageForm) model.getAttribute("timerMessageForm")).active()).isFalse();
        ArgumentCaptor<UpdateTimerMessage> captor = ArgumentCaptor.forClass(UpdateTimerMessage.class);
        org.mockito.BDDMockito.then(manageTimerMessageUseCase)
                .should().updateTimerMessage(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        then(captor.getValue().active()).isFalse();
        then(captor.getValue().messageTemplate()).isNull();
        then(captor.getValue().intervalMinutes()).isNull();
        then(captor.getValue().minChatCount()).isNull();
    }

    private TimerMessageResult timer(Long id, boolean active) {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        return new TimerMessageResult(
                id,
                "현재 시각은 {time.time}입니다.",
                30,
                10,
                active,
                7,
                now.minus(Duration.ofMinutes(30)),
                active ? now.plus(Duration.ofMinutes(30)) : null,
                "admin",
                "admin"
        );
    }
}
