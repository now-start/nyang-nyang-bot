package org.nowstart.nyangnyangbot.application.service.timer;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.doReturn;
import static org.mockito.BDDMockito.given;

import jakarta.validation.Validation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.CreateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.PreviewTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.UpdateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.in.timer.ManageTimerMessageUseCase.ValidateTimerMessage;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.MessageCommand;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.ClaimedTimerMessage;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.CreateData;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.TimerMessageRecord;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort.UpdateData;
import org.nowstart.nyangnyangbot.application.service.command.CommandTemplateRenderer;
import org.nowstart.nyangnyangbot.application.service.command.CommandVariableRegistry;
import org.nowstart.nyangnyangbot.application.service.command.CoreCommandVariableContributor;
import org.nowstart.nyangnyangbot.application.validation.UseCaseValidator;

@ExtendWith(MockitoExtension.class)
class TimerMessageServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-16T03:00:00Z");

    @Mock
    private TimerMessagePort timerMessagePort;

    @Mock
    private ChzzkClientPort chzzkClientPort;

    @Test
    void createTimerMessage_ShouldScheduleActiveTimerFromCreationTime() {
        TimerMessageService service = spyService();
        given(timerMessagePort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(1L, data.messageTemplate(), data.intervalMinutes(), data.minChatCount(),
                    data.active(), 0, null, data.nextRunAt());
        });

        var result = service.createTimerMessage(new CreateTimerMessage(
                "현재 시각은 {time.time}입니다.",
                10,
                5,
                true,
                "admin-1"
        ));

        ArgumentCaptor<CreateData> captor = ArgumentCaptor.forClass(CreateData.class);
        BDDMockito.then(timerMessagePort).should().create(captor.capture());
        then(captor.getValue().nextRunAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        then(result.active()).isTrue();
    }

    @Test
    void createTimerMessage_ShouldKeepInactiveTimerUnscheduled() {
        TimerMessageService service = spyService();
        given(timerMessagePort.create(any(CreateData.class))).willAnswer(invocation -> {
            CreateData data = invocation.getArgument(0);
            return record(1L, data.messageTemplate(), data.intervalMinutes(), data.minChatCount(),
                    data.active(), 0, null, data.nextRunAt());
        });

        service.createTimerMessage(new CreateTimerMessage("공지", 10, 5, false, "admin-1"));

        ArgumentCaptor<CreateData> captor = ArgumentCaptor.forClass(CreateData.class);
        BDDMockito.then(timerMessagePort).should().create(captor.capture());
        then(captor.getValue().nextRunAt()).isNull();
    }

    @Test
    void updateTimerMessage_ShouldResetScheduleWhenActivating() {
        TimerMessageService service = spyService();
        given(timerMessagePort.findByIdForUpdate(1L)).willReturn(Optional.of(
                record(1L, "공지", 10, 5, false, 7, null, null)
        ));
        given(timerMessagePort.update(any(UpdateData.class))).willAnswer(invocation -> {
            UpdateData data = invocation.getArgument(0);
            return record(data.id(), data.messageTemplate(), data.intervalMinutes(), data.minChatCount(),
                    data.active(), 0, null, data.nextRunAt());
        });

        service.updateTimerMessage(1L, new UpdateTimerMessage(null, null, null, true, "admin-1"));

        ArgumentCaptor<UpdateData> captor = ArgumentCaptor.forClass(UpdateData.class);
        BDDMockito.then(timerMessagePort).should().update(captor.capture());
        then(captor.getValue().resetSchedule()).isTrue();
        then(captor.getValue().nextRunAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void validate_ShouldAllowOnlyTimeVariables() {
        TimerMessageService service = spyService();

        var valid = service.validate(new ValidateTimerMessage("현재 {time.datetime}", 10, 5));
        var invalid = service.validate(new ValidateTimerMessage("안녕 {viewer.nickname}", 10, 5));

        then(valid.valid()).isTrue();
        then(invalid.valid()).isFalse();
        then(invalid.errors()).contains("timer messageTemplate cannot use variables: viewer.nickname");
    }

    @Test
    void validate_ShouldRejectOutOfRangeIntervalAndChatCount() {
        TimerMessageService service = spyService();

        var result = service.validate(new ValidateTimerMessage("공지", 4, 10_001));

        then(result.valid()).isFalse();
        then(result.errors()).contains(
                "intervalMinutes must be between 5 and 1440",
                "minChatCount must be between 1 and 10000"
        );
    }

    @Test
    void preview_ShouldRenderTimeVariableAtCurrentTime() {
        TimerMessageService service = spyService();

        var result = service.preview(new PreviewTimerMessage("지금은 {time.datetime}"));

        then(result.message()).isEqualTo("지금은 2026-07-16 12:00");
    }

    @Test
    void getTimerMessages_ShouldKeepStoredSeoulTimesForDisplay() {
        TimerMessageService service = spyService();
        given(timerMessagePort.findAllOrderByIdDesc()).willReturn(List.of(
                record(1L, "공지", 10, 5, true, 0,
                        NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofMinutes(10)))
        ));

        var result = service.getTimerMessages().getFirst();

        then(result.lastSentAt()).isEqualTo(NOW.minus(Duration.ofMinutes(5)));
        then(result.nextRunAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
    }

    @Test
    void runDueTimerMessages_ShouldCompleteClaimAfterSending() {
        TimerMessageService service = spyService();
        given(timerMessagePort.findClaimCandidateIds(NOW, 100)).willReturn(List.of(1L));
        given(timerMessagePort.claimDue(1L, "claim-1", NOW, NOW.plus(Duration.ofMinutes(2))))
                .willReturn(Optional.of(new ClaimedTimerMessage(
                        1L,
                        "현재 {time.time}",
                        10,
                        NOW,
                        "claim-1"
                )));

        service.runDueTimerMessages();

        BDDMockito.then(chzzkClientPort).should().sendMessage(new MessageCommand("현재 12:00"));
        BDDMockito.then(timerMessagePort).should().completeClaim(
                1L,
                "claim-1",
                NOW,
                10,
                NOW,
                NOW.plus(Duration.ofMinutes(10))
        );
    }

    @Test
    void runDueTimerMessages_ShouldReleaseFailedClaimAndContinueWithNextTimer() {
        TimerMessageService service = spyService();
        given(timerMessagePort.findClaimCandidateIds(NOW, 100)).willReturn(List.of(1L, 2L));
        given(timerMessagePort.claimDue(1L, "claim-1", NOW, NOW.plus(Duration.ofMinutes(2))))
                .willReturn(Optional.of(new ClaimedTimerMessage(1L, "첫 번째", 10, NOW, "claim-1")));
        given(timerMessagePort.claimDue(2L, "claim-2", NOW, NOW.plus(Duration.ofMinutes(2))))
                .willReturn(Optional.of(new ClaimedTimerMessage(2L, "두 번째", 20, NOW, "claim-2")));
        BDDMockito.willThrow(new IllegalStateException("send failed"))
                .willDoNothing()
                .given(chzzkClientPort).sendMessage(any(MessageCommand.class));

        service.runDueTimerMessages();

        BDDMockito.then(timerMessagePort).should().releaseClaim(
                1L,
                "claim-1",
                NOW,
                10,
                NOW.plus(Duration.ofMinutes(1))
        );
        BDDMockito.then(timerMessagePort).should().completeClaim(
                2L,
                "claim-2",
                NOW,
                20,
                NOW,
                NOW.plus(Duration.ofMinutes(20))
        );
    }

    @Test
    void createTimerMessage_ShouldRejectUnknownVariable() {
        TimerMessageService service = spyService();

        thenThrownBy(() -> service.createTimerMessage(
                new CreateTimerMessage("{time.unknown}", 10, 5, true, "admin-1")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unknown template variables: time.unknown");
    }

    private TimerMessageService spyService() {
        TimerMessageService service = BDDMockito.spy(service());
        BDDMockito.lenient().doReturn(NOW).when(service).currentTime();
        BDDMockito.lenient().doReturn("claim-1", "claim-2").when(service).newClaimToken();
        return service;
    }

    private TimerMessageService service() {
        CommandVariableRegistry variableRegistry = new CommandVariableRegistry(List.of(
                new CoreCommandVariableContributor()
        ));
        UseCaseValidator validator = new UseCaseValidator(
                Validation.buildDefaultValidatorFactory().getValidator()
        );
        return new TimerMessageService(
                timerMessagePort,
                chzzkClientPort,
                new CommandTemplateRenderer(),
                variableRegistry,
                validator
        );
    }

    private TimerMessageRecord record(
            Long id,
            String template,
            Integer interval,
            Integer minChatCount,
            boolean active,
            long chatCount,
            Instant lastSentAt,
            Instant nextRunAt
    ) {
        return new TimerMessageRecord(
                id,
                template,
                interval,
                minChatCount,
                active,
                chatCount,
                lastSentAt,
                nextRunAt,
                "system",
                "system"
        );
    }
}
