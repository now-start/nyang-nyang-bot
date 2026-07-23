package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;

class OverlayDisplayJobTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-23T00:00:00Z");

    @Test
    void claimUsesLeaseTokenAndCompletionClearsLease() {
        OverlayDisplayJob job = pendingJob();

        job.claim("claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(31));
        job.markDisplayed("claim-1", CREATED_AT.plusSeconds(10));

        assertThat(job.getStatus()).isEqualTo(OverlayDisplayStatus.DISPLAYED);
        assertThat(job.getClaimToken()).isNull();
        assertThat(job.getClaimExpiresAt()).isNull();
        assertThat(job.getDisplayedAt()).isEqualTo(CREATED_AT.plusSeconds(10));
    }

    @Test
    void completionRejectsDifferentClaimToken() {
        OverlayDisplayJob job = pendingJob();
        job.claim("claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(31));

        assertThatThrownBy(() -> job.markDisplayed("claim-2", CREATED_AT.plusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("overlay display claim token does not match");
    }

    @Test
    void expiredLeaseCanBeReclaimed() {
        OverlayDisplayJob job = pendingJob();
        job.claim("claim-1", CREATED_AT.plusSeconds(1), CREATED_AT.plusSeconds(10));

        job.claim("claim-2", CREATED_AT.plusSeconds(10), CREATED_AT.plusSeconds(30));

        assertThat(job.getClaimToken()).isEqualTo("claim-2");
    }

    @Test
    void completionRejectsExpiredDisplayJobEvenWhenClaimLeaseRemains() {
        OverlayDisplayJob job = pendingJob();
        job.claim("claim-1", CREATED_AT.plusSeconds(119), CREATED_AT.plusSeconds(149));

        assertThatThrownBy(() -> job.markDisplayed("claim-1", CREATED_AT.plusSeconds(121)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("overlay display job has expired");
    }

    private OverlayDisplayJob pendingJob() {
        return OverlayDisplayJob.builder()
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(CREATED_AT.plusSeconds(120))
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT)
                .build();
    }
}
