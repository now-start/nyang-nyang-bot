package org.nowstart.nyangnyangbot.application.port.in.chzzk;

public interface HandleChzzkSystemEventUseCase {

    void handle(long connectionAttemptId, SystemReceived event);

    void handleConnectionClosed(long connectionAttemptId);

    record SystemReceived(
            String type,
            SystemData data
    ) {

        public record SystemData(
                String sessionKey,
                String eventType,
                String channelId
        ) {
        }
    }
}
