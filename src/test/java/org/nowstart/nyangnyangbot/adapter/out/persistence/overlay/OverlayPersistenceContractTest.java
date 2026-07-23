package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayAccessTokenRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayJobRepository;
import org.springframework.data.jpa.repository.Lock;

class OverlayPersistenceContractTest {

    @Test
    void tokenRevocationFlushesBeforeCallerCanInsertReplacement() {
        OverlayAccessTokenRepository repository = Mockito.mock(OverlayAccessTokenRepository.class);
        OverlayTokenPersistenceAdapter adapter = new OverlayTokenPersistenceAdapter(
                repository,
                Mockito.mock(EntityManager.class)
        );
        Instant revokedAt = Instant.parse("2026-07-23T00:00:00Z");

        adapter.revokeActiveAndFlush(revokedAt);

        InOrder order = Mockito.inOrder(repository);
        order.verify(repository).revokeActive(revokedAt);
        order.verify(repository).flush();
    }

    @Test
    void claimCandidateQueryOwnsPessimisticWriteLock() throws NoSuchMethodException {
        var method = OverlayDisplayJobRepository.class.getMethod(
                "findClaimableForUpdate",
                Instant.class,
                org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus.class,
                org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus.class,
                org.springframework.data.domain.Pageable.class
        );

        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
