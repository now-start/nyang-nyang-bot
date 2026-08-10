package org.nowstart.nyangnyangbot.adapter.out.socket.chzzk;

import static io.socket.client.IO.Options;
import static io.socket.client.IO.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.adapter.out.socket.chzzk.ChzzkSocketPayloads.ChatPayload;
import org.nowstart.nyangnyangbot.adapter.out.socket.chzzk.ChzzkSocketPayloads.DonationPayload;
import org.nowstart.nyangnyangbot.adapter.out.socket.chzzk.ChzzkSocketPayloads.SystemPayload;
import org.nowstart.nyangnyangbot.application.port.in.chat.HandleChatEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkSystemEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.donation.HandleDonationEventUseCase;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkChatSocketPort;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChzzkChatConnector implements ChzzkChatSocketPort {

    private static final String SYSTEM_EVENT_NAME = "SYSTEM";
    private static final String CHAT_EVENT_NAME = "CHAT";
    private static final String DONATION_EVENT_NAME = "DONATION";
    private final HandleChzzkSystemEventUseCase handleChzzkSystemEventUseCase;
    private final HandleChatEventUseCase handleChatEventUseCase;
    private final HandleDonationEventUseCase handleDonationEventUseCase;
    private final ObjectMapper objectMapper;
    private long activeConnectionAttemptId;
    private Socket socket;

    @Override
    public synchronized void connect(String sessionUrl, long connectionAttemptId) {
        log.info("[ChzzkChat][START]");
        activeConnectionAttemptId = connectionAttemptId;

        if (socket != null) {
            socket.off();
            socket.disconnect();
        }

        Options option = new Options();
        option.reconnection = false;

        socket = createSocket(sessionUrl, option);
        socket.on(SYSTEM_EVENT_NAME, objects -> handleSystemEvent(connectionAttemptId, objects));
        socket.on(CHAT_EVENT_NAME, objects -> handleChatEvent(connectionAttemptId, objects));
        socket.on(DONATION_EVENT_NAME, objects -> handleDonationEvent(connectionAttemptId, objects));
        socket.on(Socket.EVENT_CONNECT_ERROR, objects -> handleConnectionClosed(connectionAttemptId));
        socket.on(Socket.EVENT_CONNECT_TIMEOUT, objects -> handleConnectionClosed(connectionAttemptId));
        socket.on(Socket.EVENT_DISCONNECT, objects -> handleConnectionClosed(connectionAttemptId));
        socket.connect();
    }

    Socket createSocket(String sessionUrl, Options options) {
        try {
            return socket(sessionUrl, options);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid CHZZK chat session URL", ex);
        }
    }

    private synchronized void handleSystemEvent(long connectionAttemptId, Object... objects) {
        if (connectionAttemptId != activeConnectionAttemptId) {
            return;
        }
        SystemPayload payload = readPayload(SYSTEM_EVENT_NAME, objects, SystemPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleChzzkSystemEventUseCase.handle(connectionAttemptId, payload.toEvent());
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][SYSTEM] event handling failed", ex);
        }
    }

    private synchronized void handleChatEvent(long connectionAttemptId, Object... objects) {
        if (connectionAttemptId != activeConnectionAttemptId) {
            return;
        }
        ChatPayload payload = readPayload(CHAT_EVENT_NAME, objects, ChatPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleChatEventUseCase.handle(payload.toEvent());
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][CHAT] event handling failed", ex);
        }
    }

    private synchronized void handleDonationEvent(long connectionAttemptId, Object... objects) {
        if (connectionAttemptId != activeConnectionAttemptId) {
            return;
        }
        DonationPayload payload = readPayload(DONATION_EVENT_NAME, objects, DonationPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleDonationEventUseCase.handle(payload.toEvent(nextDonationIngestionKey()));
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][DONATION] event handling failed", ex);
        }
    }

    private synchronized void handleConnectionClosed(long connectionAttemptId) {
        if (connectionAttemptId == activeConnectionAttemptId) {
            activeConnectionAttemptId = 0;
        }
        handleChzzkSystemEventUseCase.handleConnectionClosed(connectionAttemptId);
    }

    String nextDonationIngestionKey() {
        return "chzzk-received:" + UUID.randomUUID();
    }

    private <T> T readPayload(String eventName, Object[] objects, Class<T> payloadType) {
        if (objects == null || objects.length == 0 || !(objects[0] instanceof String json)) {
            log.warn("[ChzzkChat][{}] ignored non-text payload", eventName);
            return null;
        }
        try {
            return objectMapper.readValue(json, payloadType);
        } catch (JsonProcessingException ex) {
            log.warn("[ChzzkChat][{}] ignored malformed payload", eventName);
            return null;
        }
    }
}
