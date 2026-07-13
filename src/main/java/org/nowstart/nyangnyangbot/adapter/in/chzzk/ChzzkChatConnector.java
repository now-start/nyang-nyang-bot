package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static io.socket.client.IO.Options;
import static io.socket.client.IO.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Socket;
import java.net.URISyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase;
import org.nowstart.nyangnyangbot.adapter.in.chzzk.ChzzkSocketPayloads.ChatPayload;
import org.nowstart.nyangnyangbot.adapter.in.chzzk.ChzzkSocketPayloads.DonationPayload;
import org.nowstart.nyangnyangbot.adapter.in.chzzk.ChzzkSocketPayloads.SystemPayload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChzzkChatConnector implements ConnectChzzkChatSocketUseCase {

    private static final String SYSTEM_EVENT_NAME = "SYSTEM";
    private static final String CHAT_EVENT_NAME = "CHAT";
    private static final String DONATION_EVENT_NAME = "DONATION";
    private final ConnectChzzkChatUseCase connectChzzkChatUseCase;
    private final HandleChzzkEventUseCase handleChzzkEventUseCase;
    private final ObjectMapper objectMapper;
    private Socket socket;

    @Override
    public synchronized void connect() throws URISyntaxException {
        if (connectChzzkChatUseCase.isConnected()) {
            return;
        }

        log.info("[ChzzkChat][START]");

        if (socket != null) {
            socket.disconnect();
        }

        Options option = new Options();
        option.reconnection = false;

        socket = createSocket(connectChzzkChatUseCase.getSession(), option);

        socket.on(SYSTEM_EVENT_NAME, this::handleSystemEvent);
        socket.on(CHAT_EVENT_NAME, this::handleChatEvent);
        socket.connect();
    }

    Socket createSocket(String sessionUrl, Options options) throws URISyntaxException {
        return socket(sessionUrl, options);
    }

    private void handleSystemEvent(Object... objects) {
        SystemPayload payload = readPayload(SYSTEM_EVENT_NAME, objects, SystemPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleChzzkEventUseCase.handleSystemEvent(payload.toEvent());
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][SYSTEM] event handling failed", ex);
        }
    }

    private void handleChatEvent(Object... objects) {
        ChatPayload payload = readPayload(CHAT_EVENT_NAME, objects, ChatPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleChzzkEventUseCase.handleChatEvent(payload.toEvent());
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][CHAT] event handling failed", ex);
        }
    }

    private void handleDonationEvent(Object... objects) {
        DonationPayload payload = readPayload(DONATION_EVENT_NAME, objects, DonationPayload.class);
        if (payload == null) {
            return;
        }
        try {
            handleChzzkEventUseCase.handleDonationEvent(payload.toEvent());
        } catch (RuntimeException ex) {
            log.error("[ChzzkChat][DONATION] event handling failed", ex);
        }
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
