package org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class TimerMessageTest {

    @Test
    void update_ShouldPreserveInFlightClaimUntilSenderCompletesIt() {
        LocalDateTime claimExpiresAt = LocalDateTime.of(2026, 7, 16, 21, 2);
        TimerMessage timer = TimerMessage.builder()
                .messageTemplate("이전 메시지")
                .intervalMinutes(30)
                .minChatCount(10)
                .active(true)
                .chatCountSinceLastSend(12)
                .claimedChatCount(10)
                .claimToken("claim-1")
                .claimExpiresAt(claimExpiresAt)
                .createdBy("admin")
                .updatedBy("admin")
                .build();

        timer.update(
                "수정한 메시지",
                30,
                10,
                true,
                LocalDateTime.of(2026, 7, 16, 21, 30),
                false,
                "editor"
        );

        then(timer.getClaimToken()).isEqualTo("claim-1");
        then(timer.getClaimExpiresAt()).isEqualTo(claimExpiresAt);
        then(timer.getClaimedChatCount()).isEqualTo(10);
        then(timer.getChatCountSinceLastSend()).isEqualTo(12);
    }
}
