package org.nowstart.nyangnyangbot.data.dto.chzzk;

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
