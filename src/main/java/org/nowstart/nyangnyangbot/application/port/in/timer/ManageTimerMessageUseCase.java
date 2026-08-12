package org.nowstart.nyangnyangbot.application.port.in.timer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.nowstart.nyangnyangbot.domain.timer.TimerMessagePolicy;

public interface ManageTimerMessageUseCase {

    int MAX_TEMPLATE_LENGTH = TimerMessagePolicy.MAX_TEMPLATE_LENGTH;
    int DEFAULT_INTERVAL_MINUTES = TimerMessagePolicy.DEFAULT_INTERVAL_MINUTES;
    int DEFAULT_MIN_CHAT_COUNT = TimerMessagePolicy.DEFAULT_MIN_CHAT_COUNT;
    int MIN_INTERVAL_MINUTES = TimerMessagePolicy.MIN_INTERVAL_MINUTES;
    int MAX_INTERVAL_MINUTES = TimerMessagePolicy.MAX_INTERVAL_MINUTES;
    int MIN_CHAT_COUNT = TimerMessagePolicy.MIN_CHAT_COUNT;
    int MAX_CHAT_COUNT = TimerMessagePolicy.MAX_CHAT_COUNT;
    String TEMPLATE_LENGTH_MESSAGE = TimerMessagePolicy.TEMPLATE_LENGTH_MESSAGE;
    String INTERVAL_RANGE_MESSAGE = TimerMessagePolicy.INTERVAL_RANGE_MESSAGE;
    String CHAT_COUNT_RANGE_MESSAGE = TimerMessagePolicy.CHAT_COUNT_RANGE_MESSAGE;

    /** 모든 타이머 메시지를 관리 화면 표시 순서로 반환한다. */
    List<TimerMessageResult> getTimerMessages();

    /** 타이머 메시지 템플릿에서 사용할 수 있는 변수를 반환한다. */
    List<VariableResult> getVariables();

    /** 타이머 메시지를 생성하고 실행 일정을 초기화한다. */
    TimerMessageResult createTimerMessage(
            @Valid @NotNull(message = "timerMessage is required") CreateTimerMessage request
    );

    /** 지정한 타이머 메시지를 수정하고 필요한 경우 실행 일정을 다시 계산한다. */
    TimerMessageResult updateTimerMessage(
            @NotNull(message = "timerMessageId is required")
            @Positive(message = "timerMessageId must be positive") Long timerMessageId,
            @Valid @NotNull(message = "timerMessage is required") UpdateTimerMessage request
    );

    /** 전체 타이머 입력을 검증하고 저장하지 않은 채 메시지 템플릿을 렌더링한다. */
    PreviewResult preview(
            @Valid @NotNull(message = "preview is required") PreviewTimerMessage request
    );

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
            String messageTemplate,
            @Min(value = MIN_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            @Max(value = MAX_INTERVAL_MINUTES, message = INTERVAL_RANGE_MESSAGE)
            Integer intervalMinutes,
            @Min(value = MIN_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
            @Max(value = MAX_CHAT_COUNT, message = CHAT_COUNT_RANGE_MESSAGE)
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
}
