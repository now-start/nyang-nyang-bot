package org.nowstart.nyangnyangbot.adapter.in.web.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.CommandForm;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.CommandView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.ReviewView;
import org.nowstart.nyangnyangbot.adapter.in.web.command.CommandController.VariableGroupView;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.PreviewResult;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.UpdateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidationResult;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;

@ExtendWith(MockitoExtension.class)
class CommandControllerTest {

    @Mock
    private ManageCommandUseCase manageCommandUseCase;

    @InjectMocks
    private CommandController controller;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @Test
    void list_ShouldReturnFilteredCommandListFragment() {
        // 준비
        given(manageCommandUseCase.getCommands()).willReturn(List.of(
                command(1L, "!공지", true),
                command(2L, "!휴식", false)
        ));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.list(true, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-list-region");
        @SuppressWarnings("unchecked")
        List<CommandView> commands = (List<CommandView>) model.getAttribute("commands");
        then(commands).hasSize(1);
        then(commands.getFirst().triggerLabel()).isEqualTo("!공지");
        then(commands.getFirst().cooldownLabel()).isEqualTo("30초");
        then(commands.getFirst().updatedByLabel()).isEqualTo("admin");
    }

    @Test
    void list_ShouldPreserveUseCaseOrder() {
        // 준비
        given(manageCommandUseCase.getCommands()).willReturn(List.of(
                command(2L, "!최근", true),
                command(1L, "!이전", true)
        ));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        controller.list(null, model);

        // 검증
        @SuppressWarnings("unchecked")
        List<CommandView> commands = (List<CommandView>) model.getAttribute("commands");
        then(commands).extracting(CommandView::id).containsExactly(2L, 1L);
    }

    @Test
    void editor_ShouldReturnSelectedCommandFormFragment() {
        // 준비
        given(manageCommandUseCase.getCommands()).willReturn(List.of(command(1L, "!공지", true)));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.editor(1L, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        CommandForm form = (CommandForm) model.getAttribute("commandForm");
        then(form.commandId()).isEqualTo(1L);
        then(form.trigger()).isEqualTo("!공지");
    }

    @Test
    void editor_ShouldReturnPlaceholderWithoutLoadingFormData() {
        // 준비
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.editor(null, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        then(model.containsAttribute("commandForm")).isFalse();
        then(model.containsAttribute("commandVariableGroups")).isFalse();
        org.mockito.BDDMockito.then(manageCommandUseCase).shouldHaveNoInteractions();
    }

    @Test
    void editor_WhenCommandDoesNotExist_ShouldRenderErrorInsteadOfThrowing() {
        // 준비
        given(manageCommandUseCase.getCommands()).willReturn(List.of());
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.editor(999L, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        then(model.getAttribute("saveError")).isEqualTo("명령어를 찾을 수 없습니다.");
        then(model.containsAttribute("commandForm")).isFalse();
    }

    @Test
    void newEditor_ShouldReturnEmptyCreateFormWithGroupedVariables() {
        // 준비
        given(manageCommandUseCase.getVariables()).willReturn(List.of(
                new ManageCommandUseCase.VariableResult(
                        "viewer.nickname",
                        "시청자 닉네임",
                        "명령어를 호출한 시청자의 닉네임",
                        "치즈냥"
                ),
                new ManageCommandUseCase.VariableResult(
                        "point.balance",
                        "호감도",
                        "명령어를 호출한 시청자의 현재 호감도",
                        "100"
                )
        ));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.newEditor(model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        CommandForm form = (CommandForm) model.getAttribute("commandForm");
        then(form.commandId()).isNull();
        @SuppressWarnings("unchecked")
        List<VariableGroupView> groups = (List<VariableGroupView>) model.getAttribute("commandVariableGroups");
        then(groups).extracting(VariableGroupView::label).containsExactly("시청자", "연동 데이터");
        then(groups.getFirst().variables()).extracting(ManageCommandUseCase.VariableResult::key)
                .containsExactly("viewer.nickname");
    }

    @Test
    void preview_ShouldValidateFullFormAndReturnCombinedReview() {
        // 준비
        given(manageCommandUseCase.validate(any())).willReturn(new ValidationResult(true, List.of()));
        given(manageCommandUseCase.preview(any())).willReturn(new PreviewResult("치즈냥님의 호감도는 100 입니다.💛"));
        CommandForm form = new CommandForm(
                null,
                "!호감도",
                "{viewer.nickname}님의 호감도는 {point.balance} 입니다.💛",
                true,
                null
        );
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.preview(form, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-review-region");
        ReviewView review = (ReviewView) model.getAttribute("reviewResult");
        then(review.valid()).isTrue();
        then(review.errors()).isEmpty();
        then(review.previewMessage()).isEqualTo("치즈냥님의 호감도는 100 입니다.💛");
        ArgumentCaptor<ValidateCommand> captor = ArgumentCaptor.forClass(ValidateCommand.class);
        org.mockito.BDDMockito.then(manageCommandUseCase).should().validate(captor.capture());
        then(captor.getValue().trigger()).isEqualTo("!호감도");
        then(captor.getValue().messageTemplate())
                .isEqualTo("{viewer.nickname}님의 호감도는 {point.balance} 입니다.💛");
        then(captor.getValue().userCooldownSeconds()).isEqualTo(30);
    }

    @Test
    void preview_ShouldReturnValidationErrorsWithoutRenderingPreview() {
        // 준비
        given(manageCommandUseCase.validate(any()))
                .willReturn(new ValidationResult(false, List.of("trigger already exists")));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.preview(CommandForm.empty(), model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-review-region");
        ReviewView review = (ReviewView) model.getAttribute("reviewResult");
        then(review.valid()).isFalse();
        then(review.errors()).containsExactly("trigger already exists");
        then(review.previewMessage()).isNull();
        org.mockito.BDDMockito.then(manageCommandUseCase)
                .should(org.mockito.Mockito.never())
                .preview(any());
    }

    @Test
    void save_ShouldReturnEditorWithOutOfBandList() {
        // 준비
        CommandResult saved = command(1L, "!공지", true);
        given(manageCommandUseCase.createCommand(any(CreateCommand.class))).willReturn(saved);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.save(CommandForm.empty(), false, null, response, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        then(model.getAttribute("saveMessage")).isEqualTo("저장됨");
        then(response.getHeader("HX-Trigger")).isEqualTo("command-list-refresh");
        ArgumentCaptor<CreateCommand> captor = ArgumentCaptor.forClass(CreateCommand.class);
        org.mockito.BDDMockito.then(manageCommandUseCase).should().createCommand(captor.capture());
        then(captor.getValue().active()).isFalse();
    }

    @Test
    void save_ShouldRenderBeanValidationFailureInEditor() {
        // 준비
        var invalid = new CreateCommand(null, null, false, null, null);
        var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(invalid);
        given(manageCommandUseCase.createCommand(any(CreateCommand.class)))
                .willThrow(new ConstraintViolationException(violations));
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.save(CommandForm.empty(), false, null, response, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        then(model.getAttribute("saveError"))
                .isEqualTo("messageTemplate is required, trigger is required");
    }

    @Test
    void save_ShouldMapTemplateCommandFields() {
        // 준비
        CommandResult saved = command(1L, "!공지", true);
        given(manageCommandUseCase.createCommand(any(CreateCommand.class))).willReturn(saved);
        CommandForm form = new CommandForm(
                null,
                "!공지",
                "{viewer.nickname}님, 오늘 공지는 ...",
                true,
                30
        );

        // 실행
        controller.save(form, true, null, response, new ConcurrentModel());

        // 검증
        ArgumentCaptor<CreateCommand> captor = ArgumentCaptor.forClass(CreateCommand.class);
        org.mockito.BDDMockito.then(manageCommandUseCase).should().createCommand(captor.capture());
        then(captor.getValue().trigger()).isEqualTo("!공지");
        then(captor.getValue().messageTemplate()).isEqualTo("{viewer.nickname}님, 오늘 공지는 ...");
        then(captor.getValue().active()).isTrue();
        then(captor.getValue().userCooldownSeconds()).isEqualTo(30);
    }

    @Test
    void deactivate_ShouldRequestOnlyActiveStateChange() {
        // 준비
        CommandResult saved = command(1L, "!공지", false);
        given(manageCommandUseCase.updateCommand(any(), any(UpdateCommand.class))).willReturn(saved);
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.deactivate(1L, null, response, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        CommandForm form = (CommandForm) model.getAttribute("commandForm");
        then(form.active()).isFalse();
        then(model.getAttribute("saveMessage")).isEqualTo("비활성화됨");
        then(response.getHeader("HX-Trigger")).isEqualTo("command-list-refresh");
        ArgumentCaptor<UpdateCommand> captor = ArgumentCaptor.forClass(UpdateCommand.class);
        org.mockito.BDDMockito.then(manageCommandUseCase).should().updateCommand(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        then(captor.getValue().active()).isFalse();
        then(captor.getValue().trigger()).isNull();
        then(captor.getValue().messageTemplate()).isNull();
        then(captor.getValue().userCooldownSeconds()).isNull();
    }

    @Test
    void deactivate_ShouldRejectNewCommandForm() {
        // 준비
        ConcurrentModel model = new ConcurrentModel();

        // 실행
        String view = controller.deactivate(null, null, response, model);

        // 검증
        then(view).isEqualTo("features/command/regions :: command-editor-region");
        then(model.getAttribute("saveError")).isEqualTo("저장된 명령어만 비활성화할 수 있습니다.");
    }

    private CommandResult command(Long id, String trigger, boolean active) {
        return new CommandResult(
                id,
                trigger,
                "{viewer.nickname}님",
                active,
                30,
                "admin",
                "admin"
        );
    }
}
