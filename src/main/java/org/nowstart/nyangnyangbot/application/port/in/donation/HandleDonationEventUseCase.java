package org.nowstart.nyangnyangbot.application.port.in.donation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public interface HandleDonationEventUseCase {

    /** 후원 이벤트를 처리하며, 동일한 수집 키로 이미 처리된 이벤트는 중복 저장하지 않는다. */
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
