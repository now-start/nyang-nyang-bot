package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionListResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.SessionResult;

public record SessionResponse(
        String url,
        Integer page,
        Integer totalCount,
        Integer totalPages,
        List<SessionData> data
) {

    public SessionResult toSessionResult() {
        return new SessionResult(url);
    }

    public SessionListResult toSessionListResult() {
        return new SessionListResult(
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

        private SessionListResult.SessionData toSessionDataResult() {
            return new SessionListResult.SessionData(
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

            private SessionListResult.SessionData.SubscribedEvents toSubscribedEventsResult() {
                return new SessionListResult.SessionData.SubscribedEvents(eventType, channelId);
            }
        }
    }
}
