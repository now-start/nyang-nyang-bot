package org.nowstart.nyangnyangbot.data.dto.chzzk;

import java.util.Map;

public record DonationDto(
        String donationType,
        String channelId,
        String donatorChannelId,
        String donatorNickname,
        String payAmount,
        String donationText,
        Map<String, String> emojis
) {
}
