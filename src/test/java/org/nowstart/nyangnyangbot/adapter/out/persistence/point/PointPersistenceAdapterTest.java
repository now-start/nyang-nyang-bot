package org.nowstart.nyangnyangbot.adapter.out.persistence.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;

class PointPersistenceAdapterTest {

    @Test
    void findByIdempotencyKey_ExposesEveryFieldNeededForExactReplayValidation() {
        PointLedgerEntryRepository ledgerRepository = Mockito.mock(PointLedgerEntryRepository.class);
        PointLedgerEntry entry = Mockito.mock(PointLedgerEntry.class);
        UserAccount user = user("user-1");
        UserAccount actor = user("admin-1");
        PointLedgerEntry correction = Mockito.mock(PointLedgerEntry.class);
        given(correction.getId()).willReturn(3L);
        given(entry.getId()).willReturn(5L);
        given(entry.getUserAccount()).willReturn(user);
        given(entry.getDelta()).willReturn(7L);
        given(entry.getSourceType()).willReturn(PointSourceType.REWARD_MANUAL);
        given(entry.getSourceReference()).willReturn("source:1");
        given(entry.getDescription()).willReturn("보상");
        given(entry.getPrivateNote()).willReturn("감사 메모");
        given(entry.getCorrectionOfEntry()).willReturn(correction);
        given(entry.getActorUser()).willReturn(actor);
        given(entry.getIdempotencyKey()).willReturn("reward:1");
        given(ledgerRepository.findByIdempotencyKey("reward:1")).willReturn(Optional.of(entry));
        PointPersistenceAdapter adapter = new PointPersistenceAdapter(
                ledgerRepository,
                Mockito.mock(UserAccountRepository.class)
        );

        var result = adapter.findByIdempotencyKey("reward:1").orElseThrow();

        then(result.sourceReference()).isEqualTo("source:1");
        then(result.privateNote()).isEqualTo("감사 메모");
        then(result.correctionOfEntryId()).isEqualTo(3L);
        then(result.actorUserId()).isEqualTo("admin-1");
    }

    private UserAccount user(String userId) {
        return UserAccount.builder().userId(userId).build();
    }
}
