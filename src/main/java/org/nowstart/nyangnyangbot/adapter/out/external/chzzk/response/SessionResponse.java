package org.nowstart.nyangnyangbot.adapter.out.external.chzzk.response;

import java.util.List;
import java.util.Objects;
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
                        ? List.of()
                        : data.stream()
                        .filter(Objects::nonNull)
                        .map(SessionData::toSessionDataResult)
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
                            ? List.of()
                            : subscribedEvents.stream()
                            .filter(Objects::nonNull)
                            .map(SubscribedEvents::toSubscribedEventsResult)
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
