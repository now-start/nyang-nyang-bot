package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import jakarta.validation.Validation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CommandResult;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.CreateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.PreviewCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.UpdateCommand;
import org.nowstart.nyangnyangbot.application.port.in.command.ManageCommandUseCase.ValidateCommand;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CommandRecord;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.CreateData;
import org.nowstart.nyangnyangbot.application.port.out.command.CommandPort.UpdateData;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private CommandPort commandPort;

    @Mock
    private PointQueryPort pointQueryPort;

    private final CommandTemplateRenderer templateRenderer = new CommandTemplateRenderer();

    @Test
    void createCommand_ShouldNormalizeTriggerAndPersistTemplateCommand() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!hello")).willReturn(Optional.empty());
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(
                    1L,
                    data.trigger(),
                    data.messageTemplate(),
                    data.active(),
                    data.userCooldownSeconds()
            );
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                " !HELLO ",
                "{viewer.nickname}님의 호감도는 {point.balance}",
                true,
                null,
                "admin-1"
        ));

        // 검증
        then(result.trigger()).isEqualTo("!hello");
        then(result.messageTemplate()).isEqualTo("{viewer.nickname}님의 호감도는 {point.balance}");
        ArgumentCaptor<CreateData> captor = ArgumentCaptor.forClass(CreateData.class);
        BDDMockito.then(commandPort).should().create(captor.capture());
        then(captor.getValue().userCooldownSeconds()).isEqualTo(30);
    }

    @Test
    void createCommand_ShouldAcceptBareTrigger() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("치하")).willReturn(Optional.empty());
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(
                    2L,
                    data.trigger(),
                    data.messageTemplate(),
                    data.active(),
                    data.userCooldownSeconds()
            );
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                " 치하 ",
                "안녕하세요 {viewer.nickname}님!",
                true,
                30,
                "admin-1"
        ));

        // 검증
        then(result.trigger()).isEqualTo("치하");
    }

    @Test
    void createCommand_ShouldDefaultToInactive() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!hello")).willReturn(Optional.empty());
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(
                    1L,
                    data.trigger(),
                    data.messageTemplate(),
                    data.active(),
                    data.userCooldownSeconds()
            );
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                "!hello",
                "{viewer.nickname}",
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.active()).isFalse();
    }

    @Test
    void createCommand_ShouldRejectDuplicateTrigger() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!호감도"))
                .willReturn(Optional.of(record(
                        1L,
                        "!호감도",
                        "{viewer.nickname}님의 호감도는 {point.balance}",
                        true,
                        30
                )));

        // 실행 및 검증
        thenThrownBy(() -> service.createCommand(new CreateCommand(
                "!호감도",
                "{viewer.nickname} {point.balance}",
                true,
                30,
                "admin-1"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trigger already exists");
    }

    @Test
    void validate_ShouldRejectUnknownNamespacedVariable() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "!테스트",
                "{viewer.nickname} {viewer.unknown}",
                30
        ));

        // 검증
        then(validation.valid()).isFalse();
        then(validation.errors()).contains("unknown template variables: viewer.unknown");
    }

    @Test
    void validate_ShouldRejectMalformedTemplateVariable() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "!테스트",
                "{viewer_name} {1bad}",
                30
        ));

        // 검증
        then(validation.valid()).isFalse();
        then(validation.errors()).anySatisfy(error ->
                then(error)
                        .contains("malformed template variables")
                        .contains("viewer_name")
                        .contains("1bad")
        );
    }

    @Test
    void validate_ShouldRejectTriggerLongerThanTwentyAndControlCharacter() {
        // 준비
        CommandService service = service();

        // 실행
        var longValidation = service.validate(new ValidateCommand(
                null,
                "!12345678901234567890",
                "{viewer.nickname}",
                30
        ));
        var controlValidation = service.validate(new ValidateCommand(
                null,
                "!\u0007공지",
                "{viewer.nickname}",
                30
        ));

        // 검증
        then(longValidation.valid()).isFalse();
        then(longValidation.errors()).contains("trigger length must be between 2 and 20");
        then(controlValidation.valid()).isFalse();
        then(controlValidation.errors()).contains("trigger must not contain control characters");
    }

    @Test
    void validate_ShouldRejectZeroCooldown() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "!공지",
                "{viewer.nickname}",
                0
        ));

        // 검증
        then(validation.valid()).isFalse();
        then(validation.errors()).contains("userCooldownSeconds must be between 5 and 3600");
    }

    @Test
    void validate_ShouldAcceptMigratedTemplateLongerThanRenderedMessageLimit() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "!긴템플릿",
                "{invocation.arg1}".repeat(50),
                30
        ));

        // 검증
        then(validation.valid()).isTrue();
        then(validation.errors()).isEmpty();
    }

    @Test
    void updateCommand_ShouldKeepOmittedFieldsAndPersistTemplate() {
        // 준비
        CommandService service = service();
        CommandRecord current = record(1L, "!호감도", "기존 {point.balance}", true, 30);
        given(commandPort.findByIdForUpdate(1L)).willReturn(Optional.of(current));
        given(commandPort.findByTrigger("!호감도")).willReturn(Optional.of(current));
        given(commandPort.update(any(UpdateData.class))).willAnswer(invocation -> {
            UpdateData data = invocation.getArgument(0);
            return record(
                    data.id(),
                    data.trigger(),
                    data.messageTemplate(),
                    data.active(),
                    data.userCooldownSeconds()
            );
        });

        // 실행
        CommandResult result = service.updateCommand(1L, new UpdateCommand(
                null,
                "{viewer.nickname}님의 호감도는 {point.balance}",
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.trigger()).isEqualTo("!호감도");
        then(result.active()).isTrue();
        then(result.userCooldownSeconds()).isEqualTo(30);
        then(result.messageTemplate()).isEqualTo("{viewer.nickname}님의 호감도는 {point.balance}");
    }

    @Test
    void preview_ShouldRenderNamespacedVariableSamplesForNyangNyangBotResponse() {
        // 준비
        CommandService service = service();

        // 실행
        var result = service.preview(new PreviewCommand(
                "{viewer.nickname} {invocation.command} {invocation.args} "
                        + "{invocation.arg1} {invocation.arg2} {point.balance}"
        ));

        // 검증
        then(result.message()).isEqualTo("치즈냥 치하 첫번째 두번째 첫번째 두번째 100");
        BDDMockito.then(pointQueryPort).shouldHaveNoInteractions();
    }

    @Test
    void getVariables_ShouldExposeCanonicalVariablesOnly() {
        // 준비
        CommandService service = service();

        // 실행
        var variables = service.getVariables();

        // 검증
        then(variables)
                .extracting(variable -> variable.key())
                .contains("viewer.nickname", "invocation.command", "point.balance")
                .doesNotContain("nickname", "command", "favorite");
    }

    @Test
    void preview_ShouldRejectRemovedLegacyVariables() {
        // 준비
        CommandService service = service();

        // 실행 및 검증
        thenThrownBy(() -> service.preview(new PreviewCommand("{nickname}님의 호감도는 {favorite}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown template variables: favorite, nickname");
    }

    private CommandService service() {
        CommandVariableRegistry variableRegistry = new CommandVariableRegistry(List.of(
                new CoreCommandVariableContributor(),
                new PointCommandVariableContributor(pointQueryPort)
        ));
        return new CommandService(commandPort, templateRenderer, variableRegistry, validator());
    }

    private UseCaseValidator validator() {
        return new UseCaseValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    private CommandRecord record(
            Long id,
            String trigger,
            String messageTemplate,
            boolean active,
            Integer cooldownSeconds
    ) {
        return new CommandRecord(
                id,
                trigger,
                messageTemplate,
                active,
                cooldownSeconds,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
