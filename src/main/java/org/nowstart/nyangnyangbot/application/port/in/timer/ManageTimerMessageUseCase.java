package org.nowstart.nyangnyangbot.application.port.in.timer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public interface ManageTimerMessageUseCase {

    int MAX_TEMPLATE_LENGTH = 1_000;

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
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate,
            @Min(value = 5, message = "intervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "intervalMinutes must be between 5 and 1440")
            Integer intervalMinutes,
            @Min(value = 1, message = "minChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "minChatCount must be between 1 and 10000")
            Integer minChatCount,
            Boolean active,
            String actorId
    ) {
    }

    record UpdateTimerMessage(
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
            String messageTemplate,
            @Min(value = 5, message = "intervalMinutes must be between 5 and 1440")
            @Max(value = 1440, message = "intervalMinutes must be between 5 and 1440")
            Integer intervalMinutes,
            @Min(value = 1, message = "minChatCount must be between 1 and 10000")
            @Max(value = 10000, message = "minChatCount must be between 1 and 10000")
            Integer minChatCount,
            Boolean active,
            String actorId
    ) {
    }

    record PreviewTimerMessage(
            @NotBlank(message = "messageTemplate is required")
            @Size(max = MAX_TEMPLATE_LENGTH, message = "messageTemplate length must be 1000 or less")
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
            LocalDateTime lastSentAt,
            LocalDateTime nextRunAt,
            String createdBy,
            String updatedBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    record VariableResult(String key, String label, String description, String example) {
    }

    record PreviewResult(String message) {
    }

    record ValidationResult(boolean valid, List<String> errors) {
    }
}
