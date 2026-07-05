package org.nowstart.nyangnyangbot.application.service.command;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import jakarta.validation.Validation;
import java.time.LocalDateTime;
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
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;
import org.nowstart.nyangnyangbot.domain.type.CommandActionKey;
import org.nowstart.nyangnyangbot.domain.type.CommandType;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private CommandPort commandPort;

    private final CommandTemplateRenderer templateRenderer = new CommandTemplateRenderer();

    @Test
    void createCommand_ShouldNormalizeTriggerAndPersistTextCommand() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!hello")).willReturn(Optional.empty());
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(1L, data.type(), data.trigger(), data.actionKey(), data.messageTemplate(),
                    data.timerIntervalMinutes(), data.timerMinChatCount());
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                "TEXT",
                " !HELLO ",
                null,
                "{nickname}님의 호감도는 {favorite}",
                null,
                null,
                true,
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.trigger()).isEqualTo("!hello");
        then(result.messageTemplate()).isEqualTo("{nickname}님의 호감도는 {favorite}");
        ArgumentCaptor<CreateData> captor = ArgumentCaptor.forClass(CreateData.class);
        BDDMockito.then(commandPort).should().create(captor.capture());
        then(captor.getValue().userCooldownSeconds()).isEqualTo(30);
    }

    @Test
    void createCommand_ShouldDefaultToInactive() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!hello")).willReturn(Optional.empty());
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(1L, data.type(), data.trigger(), data.actionKey(), data.messageTemplate(),
                    data.timerIntervalMinutes(), data.timerMinChatCount(), data.active());
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                "TEXT",
                "!hello",
                null,
                "{nickname}",
                null,
                null,
                null,
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.active()).isFalse();
    }

    @Test
    void createCommand_ShouldRejectDuplicateTriggerAndActionKey() {
        // 준비
        CommandService service = service();
        given(commandPort.findByTrigger("!호감도")).willReturn(Optional.of(record(
                1L,
                CommandType.TRIGGER,
                "!호감도",
                CommandActionKey.FAVORITE_STATUS,
                null,
                null,
                null
        )));
        given(commandPort.findByActionKey(CommandActionKey.FAVORITE_STATUS)).willReturn(Optional.of(record(
                1L,
                CommandType.TRIGGER,
                "!호감도",
                CommandActionKey.FAVORITE_STATUS,
                null,
                null,
                null
        )));

        // 실행 및 검증
        thenThrownBy(() -> service.createCommand(new CreateCommand(
                "TRIGGER",
                "!호감도",
                "FAVORITE_STATUS",
                null,
                null,
                null,
                true,
                null,
                null,
                "admin-1"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trigger already exists")
                .hasMessageContaining("actionKey already exists");
    }

    @Test
    void validate_ShouldRejectUnknownTemplateVariableAndZeroTimerMinChatCount() {
        // 준비
        CommandService service = service();

        // 실행
        var textValidation = service.validate(new ValidateCommand(
                null,
                "TEXT",
                "!테스트",
                null,
                "{nickname} {upbo}",
                null,
                null,
                null,
                null
        ));
        var timerValidation = service.validate(new ValidateCommand(
                null,
                "TIMER",
                null,
                null,
                "{datetime}",
                5,
                0,
                null,
                null
        ));

        // 검증
        then(textValidation.valid()).isFalse();
        then(textValidation.errors()).contains("unknown template variables: upbo");
        then(timerValidation.valid()).isFalse();
        then(timerValidation.errors()).contains("timerMinChatCount must be between 1 and 10000");
    }

    @Test
    void validate_ShouldRejectMalformedTemplateVariable() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "TEXT",
                "!테스트",
                null,
                "{bad_var} {1bad}",
                null,
                null,
                null,
                null
        ));

        // 검증
        then(validation.valid()).isFalse();
        then(validation.errors()).anySatisfy(error ->
                then(error).contains("unknown template variables").contains("bad_var").contains("1bad")
        );
    }

    @Test
    void validate_ShouldRejectTriggerLongerThanTwentyAndControlCharacter() {
        // 준비
        CommandService service = service();

        // 실행
        var longValidation = service.validate(new ValidateCommand(
                null,
                "TEXT",
                "!12345678901234567890",
                null,
                "{nickname}",
                null,
                null,
                null,
                null
        ));
        var controlValidation = service.validate(new ValidateCommand(
                null,
                "TEXT",
                "!\u0007공지",
                null,
                "{nickname}",
                null,
                null,
                null,
                null
        ));

        // 검증
        then(longValidation.valid()).isFalse();
        then(longValidation.errors()).contains("trigger length must be between 2 and 20");
        then(controlValidation.valid()).isFalse();
        then(controlValidation.errors()).contains("trigger must not contain control characters");
    }

    @Test
    void validate_ShouldRejectCallerVariablesForTimer() {
        // 준비
        CommandService service = service();

        // 실행
        var validation = service.validate(new ValidateCommand(
                null,
                "TIMER",
                null,
                null,
                "{datetime} {nickname} {args}",
                null,
                null,
                null,
                null
        ));

        // 검증
        then(validation.valid()).isFalse();
        then(validation.errors()).contains("timer messageTemplate cannot use caller variables: args, nickname");
    }

    @Test
    void createCommand_ShouldDefaultTimerSettings() {
        // 준비
        CommandService service = service();
        given(commandPort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(1L, data.type(), data.trigger(), data.actionKey(), data.messageTemplate(),
                    data.timerIntervalMinutes(), data.timerMinChatCount());
        });

        // 실행
        CommandResult result = service.createCommand(new CreateCommand(
                "TIMER",
                null,
                null,
                "{datetime} 공지",
                null,
                null,
                true,
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.timerIntervalMinutes()).isEqualTo(10);
        then(result.timerMinChatCount()).isEqualTo(10);
    }

    @Test
    void updateCommand_ShouldDropMessageTemplateForTriggerCommand() {
        // 준비
        CommandService service = service();
        given(commandPort.findById(1L)).willReturn(Optional.of(record(
                1L,
                CommandType.TRIGGER,
                "!호감도",
                CommandActionKey.FAVORITE_STATUS,
                null,
                null,
                null
        )));
        given(commandPort.findByTrigger("!호감도")).willReturn(Optional.of(record(
                1L,
                CommandType.TRIGGER,
                "!호감도",
                CommandActionKey.FAVORITE_STATUS,
                null,
                null,
                null
        )));
        given(commandPort.findByActionKey(CommandActionKey.FAVORITE_STATUS)).willReturn(Optional.of(record(
                1L,
                CommandType.TRIGGER,
                "!호감도",
                CommandActionKey.FAVORITE_STATUS,
                null,
                null,
                null
        )));
        given(commandPort.update(any(UpdateData.class))).willAnswer(invocation -> {
            UpdateData data = invocation.getArgument(0);
            return record(data.id(), CommandType.TRIGGER, data.trigger(), CommandActionKey.FAVORITE_STATUS,
                    data.messageTemplate(), data.timerIntervalMinutes(), data.timerMinChatCount());
        });

        // 실행
        CommandResult result = service.updateCommand(1L, new UpdateCommand(
                null,
                null,
                null,
                "{nickname}",
                null,
                null,
                null,
                null,
                null,
                "admin-1"
        ));

        // 검증
        then(result.messageTemplate()).isNull();
    }

    @Test
    void validate_ShouldRejectZeroCooldownExceptRouletteDonation() {
        // 준비
        CommandService service = service();

        // 실행
        var textValidation = service.validate(new ValidateCommand(
                null,
                "TEXT",
                "!공지",
                null,
                "{nickname}",
                null,
                null,
                null,
                0
        ));
        var rouletteDonationValidation = service.validate(new ValidateCommand(
                null,
                "TRIGGER",
                "!룰렛",
                "ROULETTE_DONATION",
                null,
                null,
                null,
                null,
                0
        ));

        // 검증
        then(textValidation.valid()).isFalse();
        then(textValidation.errors()).contains("userCooldownSeconds must be between 5 and 3600");
        then(rouletteDonationValidation.valid()).isTrue();
    }

    @Test
    void preview_ShouldRenderSupportedVariables() {
        // 준비
        CommandService service = service();

        // 실행
        var result = service.preview(new PreviewCommand(
                "{nickname} {command} {args} {arg1} {arg2} {favorite}",
                "치즈냥",
                "!점수",
                "a b",
                "a",
                "b",
                77
        ));

        // 검증
        then(result.message()).isEqualTo("치즈냥 !점수 a b a b 77");
    }

    @Test
    void preview_ShouldLimitAndNeutralizeUserProvidedVariables() {
        // 준비
        CommandService service = service();

        // 실행
        var result = service.preview(new PreviewCommand(
                "{arg1}",
                "치즈냥",
                "!점수",
                null,
                "HTTP://example.com/@everyone-WWW.example.com-abcdefghijklmnopqrstuvwxyzabcdefghijklmnop",
                null,
                null
        ));

        // 검증
        then(result.message()).startsWith("http[:]//example.com/[at]everyone-www[.]example.com");
        then(result.message()).hasSizeLessThanOrEqualTo(60);
    }

    private CommandService service() {
        return new CommandService(commandPort, templateRenderer, validator());
    }

    private UseCaseValidator validator() {
        return new UseCaseValidator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    private CommandRecord record(
            Long id,
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount
    ) {
        return new CommandRecord(
                id,
                type,
                trigger,
                actionKey,
                messageTemplate,
                timerIntervalMinutes,
                timerMinChatCount,
                true,
                "USER",
                30,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private CommandRecord record(
            Long id,
            CommandType type,
            String trigger,
            CommandActionKey actionKey,
            String messageTemplate,
            Integer timerIntervalMinutes,
            Integer timerMinChatCount,
            boolean active
    ) {
        return new CommandRecord(
                id,
                type,
                trigger,
                actionKey,
                messageTemplate,
                timerIntervalMinutes,
                timerMinChatCount,
                active,
                "USER",
                30,
                "system",
                "system",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
