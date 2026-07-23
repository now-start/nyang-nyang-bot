package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static org.assertj.core.api.BDDAssertions.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.chzzk.ChzzkSocketPayloads.DonationPayload;

class ChzzkSocketPayloadsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void donationPayload_ShouldMatchOfficialPayloadWithoutInventingProviderEventId() throws Exception {
        DonationPayload payload = objectMapper.readValue("""
                {
                  "donationType": "CHAT",
                  "channelId": "streamer-1",
                  "donatorChannelId": "viewer-1",
                  "payAmount": "1,000"
                }
                """, DonationPayload.class);

        then(payload.toEvent("chzzk-received:test").ingestionKey())
                .isEqualTo("chzzk-received:test");
        then(payload.toEvent("chzzk-received:test").payAmount()).isEqualTo("1,000");
    }
}
