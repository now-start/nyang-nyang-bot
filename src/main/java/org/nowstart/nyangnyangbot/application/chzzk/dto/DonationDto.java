package org.nowstart.nyangnyangbot.application.chzzk.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.Map;

public record DonationDto(
        @JsonAlias({"donationId", "eventId", "id"})
        String donationEventId,
        String donationType,
        String channelId,
        String donatorChannelId,
        String donatorNickname,
        String payAmount,
        String donationText,
        Map<String, String> emojis
) {
}
