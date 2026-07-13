package org.nowstart.nyangnyangbot.application.port.in.chzzk;

import java.util.List;
import java.util.Map;

public interface HandleChzzkEventUseCase {

    void handleSystemEvent(SystemReceived event);

    void handleChatEvent(ChatReceived event);

    void handleDonationEvent(DonationReceived event);

    record SystemReceived(
            String type,
            SystemData data
    ) {

        public record SystemData(
                String sessionKey,
                String eventType,
                String channelId
        ) {
        }
    }

    record ChatReceived(
            String channelId,
            String senderChannelId,
            Profile profile,
            String content,
            Map<String, String> emojis,
            Long messageTime
    ) {

        public record Profile(
                String nickname,
                List<Map<String, String>> badges,
                Boolean verifiedMark
        ) {
        }
    }

    record DonationReceived(
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

}
