package org.nowstart.nyangnyangbot.application.port.in.timer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy;

public interface ManageTimerMessageUseCase {

    int MAX_TEMPLATE_LENGTH = TimerMessagePolicy.MAX_TEMPLATE_LENGTH;
    int DEFAULT_INTERVAL_MINUTES = 30;
    int DEFAULT_MIN_CHAT_COUNT = 10;
    int MIN_INTERVAL_MINUTES = TimerMessagePolicy.MIN_INTERVAL_MINUTES;
    int MAX_INTERVAL_MINUTES = TimerMessagePolicy.MAX_INTERVAL_MINUTES;
    int MIN_CHAT_COUNT = TimerMessagePolicy.MIN_CHAT_COUNT;
    int MAX_CHAT_COUNT = TimerMessagePolicy.MAX_CHAT_COUNT;
    String TEMPLATE_LENGTH_MESSAGE = TimerMessagePolicy.TEMPLATE_LENGTH_MESSAGE;
    String INTERVAL_RANGE_MESSAGE = TimerMessagePolicy.INTERVAL_RANGE_MESSAGE;
    String CHAT_COUNT_RANGE_MESSAGE = TimerMessagePolicy.CHAT_COUNT_RANGE_MESSAGE;

    List<TimerMessageResult> getTimerMessages();

    List<VariableResult> getVariables();

    TimerMessageResult createTimerMessage(
            @Valid @NotNull(message = "timerMessage is required") CreateTimerMessage request
    );

    TimerMessageResult updateTimerMessage(
            Long timerMessageId,
            @Valid @NotNull(message = "timerMessage is required") UpdateTimerMessage request
    );

    PreviewResult preview(
            @Valid @NotNull(message = "preview is required") PreviewTimerMessage request
    );

    ValidationResult validate(ValidateTimerMessage request);

    record CreateTimerMessage(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            Integer intervalMinutes,
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            Integer minChatCount,
            Boolean active,
            String actorId
    ) {
    }

    record UpdateTimerMessage(
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate,
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            Integer intervalMinutes,
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            Integer minChatCount,
            Boolean active,
            String actorId
    ) {
    }

    record PreviewTimerMessage(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = TEMPLATE_LENGTH_MESSAGE)
            String messageTemplate
    ) {
    }

    record ValidateTimerMessage(
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount
    ) {
    }

    record TimerMessageResult(
            Long id,
            String messageTemplate,
            Integer intervalMinutes,
            Integer minChatCount,
            boolean active,
            long chatCountSinceLastSend,
            Instant lastSentAt,
            Instant nextRunAt,
            String createdBy,
            String updatedBy
    ) {
    }

    record VariableResult(String key, String label, String description, String example) {
    }

    record PreviewResult(String message) {
    }

    record ValidationResult(boolean valid, List<String> errors) {
    }
}
