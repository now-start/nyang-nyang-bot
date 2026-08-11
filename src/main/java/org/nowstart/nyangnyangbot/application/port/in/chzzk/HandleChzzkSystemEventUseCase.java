package org.nowstart.nyangnyangbot.application.port.in.chzzk;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public interface HandleChzzkSystemEventUseCase {

    void handle(
            @Positive(message = "connectionAttemptId must be positive") long connectionAttemptId,
            @Valid @NotNull(message = "system event is required") SystemReceived event
    );

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
