package org.nowstart.nyangnyangbot.adapter.in.chzzk;

import static org.assertj.core.api.BDDAssertions.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.in.chzzk.ChzzkSocketPayloads.DonationPayload;

class ChzzkSocketPayloadsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void donationPayload_ShouldAcceptProviderEventIdAlias() throws Exception {
        DonationPayload payload = objectMapper.readValue("""
                {
                  "eventId": "donation-1",
                  "payAmount": "1,000"
                }
                """, DonationPayload.class);

        then(payload.toEvent().donationEventId()).isEqualTo("donation-1");
        then(payload.toEvent().payAmount()).isEqualTo("1,000");
    }
}
