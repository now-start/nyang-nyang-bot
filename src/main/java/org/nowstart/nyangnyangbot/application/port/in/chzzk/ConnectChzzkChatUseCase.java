package org.nowstart.nyangnyangbot.application.port.in.chzzk;

import io.socket.emitter.Emitter;

public interface ConnectChzzkChatUseCase {

    String SYSTEM_EVENT_NAME = "SYSTEM";
    String CHAT_EVENT_NAME = "CHAT";
    String DONATION_EVENT_NAME = "DONATION";
    String SUBSCRIPTION_EVENT_NAME = "SUBSCRIPTION";

    boolean isConnected();

    String getSession();

    Emitter.Listener systemListener();

    Emitter.Listener chatListener();

    Emitter.Listener donationListener();

    Emitter.Listener subscriptionListener();
}
