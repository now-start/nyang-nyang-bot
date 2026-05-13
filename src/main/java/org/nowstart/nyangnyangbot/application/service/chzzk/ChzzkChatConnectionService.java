package org.nowstart.nyangnyangbot.application.service.chzzk;

import io.socket.emitter.Emitter;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.nowstart.nyangnyangbot.application.service.chat.ChatService;
import org.nowstart.nyangnyangbot.application.service.donation.DonationService;
import org.nowstart.nyangnyangbot.application.service.subscription.SubscriptionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChzzkChatConnectionService implements ConnectChzzkChatUseCase {

    private final SystemService systemService;
    private final ChatService chatService;
    private final DonationService donationService;
    private final SubscriptionService subscriptionService;

    @Override
    public boolean isConnected() {
        return systemService.isConnected();
    }

    @Override
    public String getSession() {
        return systemService.getSession();
    }

    @Override
    public Emitter.Listener systemListener() {
        return systemService;
    }

    @Override
    public Emitter.Listener chatListener() {
        return chatService;
    }

    @Override
    public Emitter.Listener donationListener() {
        return donationService;
    }

    @Override
    public Emitter.Listener subscriptionListener() {
        return subscriptionService;
    }
}
