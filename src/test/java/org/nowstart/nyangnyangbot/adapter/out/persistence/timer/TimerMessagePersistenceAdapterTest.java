package org.nowstart.nyangnyangbot.adapter.out.persistence.timer;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.nowstart.nyangnyangbot.support.OutboundContractTestSupport.outboundContractValidator;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;

class TimerMessagePersistenceAdapterTest {

    @Test
    void claimDue_ShouldRejectInvalidPersistenceResult() {
        Instant now = Instant.parse("2026-08-11T00:00:00Z");
        TimerMessageRepository repository = Mockito.mock(TimerMessageRepository.class);
        TimerMessage invalid = TimerMessage.builder()
                .id(1L)
                .messageTemplate("message")
                .intervalMinutes(1)
                .nextRunAt(now)
                .claimToken("claim-1")
                .build();
        given(repository.claimDue(1L, "claim-1", now, now.plusSeconds(120))).willReturn(1);
        given(repository.findByIdAndClaimToken(1L, "claim-1")).willReturn(Optional.of(invalid));
        TimerMessagePersistenceAdapter adapter = new TimerMessagePersistenceAdapter(
                repository,
                Mockito.mock(UserAccountRepository.class),
                outboundContractValidator()
        );

        thenThrownBy(() -> adapter.claimDue(1L, "claim-1", now, now.plusSeconds(120)))
                .isInstanceOf(PersistenceDataContractException.class)
                .hasMessageContaining("operation=timerMessage.claimed")
                .hasMessageContaining("intervalMinutes must be between 5 and 1440");
    }
}
