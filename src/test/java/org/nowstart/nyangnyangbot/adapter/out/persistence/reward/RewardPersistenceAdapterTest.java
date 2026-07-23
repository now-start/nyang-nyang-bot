package org.nowstart.nyangnyangbot.adapter.out.persistence.reward;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.repository.RewardGrantRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.springframework.data.domain.Pageable;

class RewardPersistenceAdapterTest {

    @Test
    void findByUserId_AppliesRequestedLimitAtRepositoryBoundary() {
        RewardGrantRepository repository = Mockito.mock(RewardGrantRepository.class);
        RewardPersistenceAdapter adapter = adapter(repository);
        given(repository.findByUserAccount_UserIdOrderByCreatedAtDescIdDesc(
                Mockito.eq("user-1"),
                Mockito.any(Pageable.class)
        )).willReturn(List.of());

        adapter.findByUserId("user-1", 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(repository).findByUserAccount_UserIdOrderByCreatedAtDescIdDesc(
                Mockito.eq("user-1"),
                pageable.capture()
        );
        then(pageable.getValue().getPageNumber()).isZero();
        then(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void findByUserIdAndStatus_AppliesRequestedLimitAtRepositoryBoundary() {
        RewardGrantRepository repository = Mockito.mock(RewardGrantRepository.class);
        RewardPersistenceAdapter adapter = adapter(repository);
        given(repository.findByUserAccount_UserIdAndStatusOrderByCreatedAtDescIdDesc(
                Mockito.eq("user-1"),
                Mockito.eq(RewardGrantStatus.OWNED),
                Mockito.any(Pageable.class)
        )).willReturn(List.of());

        adapter.findByUserIdAndStatus("user-1", RewardGrantStatus.OWNED, 15);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(repository).findByUserAccount_UserIdAndStatusOrderByCreatedAtDescIdDesc(
                Mockito.eq("user-1"),
                Mockito.eq(RewardGrantStatus.OWNED),
                pageable.capture()
        );
        then(pageable.getValue().getPageSize()).isEqualTo(15);
    }

    private RewardPersistenceAdapter adapter(RewardGrantRepository repository) {
        return new RewardPersistenceAdapter(
                repository,
                Mockito.mock(EntityManager.class),
                Mockito.mock(OutboundContractValidator.class)
        );
    }
}
