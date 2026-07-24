package org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimerMessageTest {

    @Test
    void update_ShouldPreserveInFlightClaimUntilSenderCompletesIt() {
        Instant claimExpiresAt = Instant.parse("2026-07-16T12:02:00Z");
        TimerMessage timer = TimerMessage.builder()
                .messageTemplate("이전 메시지")
                .intervalMinutes(30)
                .minChatCount(10)
                .active(true)
                .chatCountSinceLastSend(12)
                .claimedChatCount(10)
                .claimToken("claim-1")
                .claimExpiresAt(claimExpiresAt)
                .build();

        timer.update(
                "수정한 메시지",
                30,
                10,
                true,
                claimExpiresAt.plus(Duration.ofMinutes(28)),
                false,
                null
        );

        then(timer.getClaimToken()).isEqualTo("claim-1");
        then(timer.getClaimExpiresAt()).isEqualTo(claimExpiresAt);
        then(timer.getClaimedChatCount()).isEqualTo(10);
        then(timer.getChatCountSinceLastSend()).isEqualTo(12);
    }
}
