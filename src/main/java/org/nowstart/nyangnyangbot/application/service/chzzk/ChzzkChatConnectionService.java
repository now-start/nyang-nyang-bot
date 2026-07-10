package org.nowstart.nyangnyangbot.application.service.chzzk;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SubscriptionReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SystemReceived;
import org.nowstart.nyangnyangbot.application.service.chat.ChatService;
import org.nowstart.nyangnyangbot.application.service.donation.DonationService;
import org.nowstart.nyangnyangbot.application.service.subscription.SubscriptionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChzzkChatConnectionService implements ConnectChzzkChatUseCase, HandleChzzkEventUseCase {

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
    public void handleSystemEvent(SystemReceived event) {
        systemService.handle(event);
    }

    @Override
    public void handleChatEvent(ChatReceived event) {
        chatService.handle(event);
    }

    @Override
    public void handleDonationEvent(DonationReceived event) {
        donationService.handle(event);
    }

    @Override
    public void handleSubscriptionEvent(SubscriptionReceived event) {
        subscriptionService.handle(event);
    }
}
