package org.nowstart.nyangnyangbot.application.port.in.donation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public interface HandleDonationEventUseCase {

    void handle(@Valid @NotNull(message = "donation is required") DonationReceived event);

    record DonationReceived(
            @NotBlank(message = "ingestionKey is required") String ingestionKey,
            String donationType,
            @NotBlank(message = "channelId is required") String channelId,
            String donatorChannelId,
            String donatorNickname,
            @NotBlank(message = "payAmount is required") String payAmount,
            String donationText,
            Map<String, String> emojis
    ) {
    }
}
