package org.nowstart.nyangnyangbot.application.port.in.chzzk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface HandleChzzkSystemEventUseCase {

    /** 지정한 연결 시도에서 발생한 시스템 이벤트만 처리한다. */
    void handle(
            @Positive(message = "connectionAttemptId must be positive") long connectionAttemptId,
            @Valid @NotNull(message = "system event is required") SystemReceived event
    );

    /** 종료된 소켓이 현재 연결 시도에 속하면 연결 상태를 초기화한다. */
    void handleConnectionClosed(
            @Positive(message = "connectionAttemptId must be positive") long connectionAttemptId
    );

    record SystemReceived(
            @NotBlank(message = "type is required") String type,
            @Valid @NotNull(message = "system data is required") SystemData data
    ) {

        public record SystemData(
                String sessionKey,
                String eventType,
                String channelId
        ) {
        }
    }
}
