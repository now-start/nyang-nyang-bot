package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import java.util.List;
import java.util.Map;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.SystemReceived;

final class ChzzkSocketPayloads {

    private ChzzkSocketPayloads() {
    }

    record ChatPayload(
            String channelId,
            String senderChannelId,
            Profile profile,
            String content,
            Map<String, String> emojis,
            Long messageTime
    ) {

        ChatReceived toEvent() {
            return new ChatReceived(
                    channelId,
                    senderChannelId,
                    profile == null ? null : profile.toEvent(),
                    content,
                    emojis,
                    messageTime
            );
        }

        record Profile(
                String nickname,
                List<Map<String, String>> badges,
                Boolean verifiedMark
        ) {

            ChatReceived.Profile toEvent() {
                return new ChatReceived.Profile(nickname, badges, verifiedMark);
            }
        }
    }

    record SystemPayload(
            String type,
            SystemData data
    ) {

        SystemReceived toEvent() {
            return new SystemReceived(type, data == null ? null : data.toEvent());
        }

        record SystemData(
                String sessionKey,
                String eventType,
                String channelId
        ) {

            SystemReceived.SystemData toEvent() {
                return new SystemReceived.SystemData(sessionKey, eventType, channelId);
            }
        }
    }

    record DonationPayload(
            String donationType,
            String channelId,
            String donatorChannelId,
            String donatorNickname,
            String payAmount,
            String donationText,
            Map<String, String> emojis
    ) {

        DonationReceived toEvent(String ingestionKey) {
            return new DonationReceived(
                    ingestionKey,
                    donationType,
                    channelId,
                    donatorChannelId,
                    donatorNickname,
                    payAmount,
                    donationText,
                    emojis
            );
        }
    }

}
