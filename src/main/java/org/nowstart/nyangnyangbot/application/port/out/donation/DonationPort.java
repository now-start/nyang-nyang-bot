package org.nowstart.nyangnyangbot.application.port.out.donation;

import java.util.Map;

public interface DonationPort {

    boolean existsByDonationEventId(String donationEventId);

    void save(SaveDonationCommand command);

    record SaveDonationCommand(
            String donationEventId,
            String donationType,
            String channelId,
            String donatorChannelId,
            String donatorNickname,
            Long payAmount,
            String donationText,
            Map<String, String> emojis
    ) {
    }
}
