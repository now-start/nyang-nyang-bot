package org.nowstart.nyangnyangbot.application.port.out.donation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
            @NotNull(message = "payAmount is required")
            @PositiveOrZero(message = "payAmount must not be negative")
            Long payAmount,
            String donationText,
            Map<String, String> emojis
    ) {
    }
}
