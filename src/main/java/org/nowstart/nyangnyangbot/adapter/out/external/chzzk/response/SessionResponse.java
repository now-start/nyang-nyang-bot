package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionResult;

public record SessionResponse(
        String url,
        Integer page,
        Integer totalCount,
        Integer totalPages,
        List<SessionData> data
) {

    public SessionResult toSessionResult() {
        return new SessionResult(
                url,
                page,
                totalCount,
                totalPages,
                data == null
                        ? null
                        : data.stream()
                        .map(session -> session == null ? null : session.toSessionDataResult())
                        .toList()
        );
    }

    public record SessionData(
            String sessionKey,
            String connectedDate,
            String disconnectedDate,
            List<SubscribedEvents> subscribedEvents
    ) {

        private SessionResult.SessionData toSessionDataResult() {
            return new SessionResult.SessionData(
                    sessionKey,
                    connectedDate,
                    disconnectedDate,
                    subscribedEvents == null
                            ? null
                            : subscribedEvents.stream()
                            .map(event -> event == null ? null : event.toSubscribedEventsResult())
                            .toList()
            );
        }

        public record SubscribedEvents(String eventType, String channelId) {

            private SessionResult.SessionData.SubscribedEvents toSubscribedEventsResult() {
                return new SessionResult.SessionData.SubscribedEvents(eventType, channelId);
            }
        }
    }
}
