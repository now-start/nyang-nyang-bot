package org.nowstart.nyangnyangbot.application.port.in.donation;

import java.util.Map;

public interface HandleDonationEventUseCase {

    void handle(DonationReceived event);

    record DonationReceived(
            String ingestionKey,
            String donationType,
            String channelId,
            String donatorChannelId,
            String donatorNickname,
            String payAmount,
            String donationText,
            Map<String, String> emojis
    ) {
    }
}
