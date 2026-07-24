package org.nowstart.nyangnyangbot.adapter.out.persistence.reward;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointLedgerEntry;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.entity.RewardGrant;
import org.nowstart.nyangnyangbot.adapter.out.persistence.reward.repository.RewardGrantRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRound;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.reward.RewardPort.CreateRewardCommand;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardGrantStatus;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.springframework.data.domain.Pageable;

class RewardPersistenceAdapterTest {

    @Test
    void createGrantPersistsWithoutBuildingAQueryRecord() {
        RewardGrantRepository repository = Mockito.mock(RewardGrantRepository.class);
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        OutboundContractValidator validator = Mockito.mock(OutboundContractValidator.class);
        given(entityManager.getReference(UserAccount.class, "user-1")).willReturn(Mockito.mock(UserAccount.class));
        given(entityManager.getReference(RouletteRound.class, 10L)).willReturn(Mockito.mock(RouletteRound.class));
        given(entityManager.getReference(PointLedgerEntry.class, 20L))
                .willReturn(Mockito.mock(PointLedgerEntry.class));
        RewardPersistenceAdapter adapter = new RewardPersistenceAdapter(repository, entityManager, validator);

        adapter.createGrant(new CreateRewardCommand(
                "user-1",
                10L,
                20L,
                "포인트",
                RewardType.POINT,
                ConversionMode.AUTO,
                100L,
                RewardGrantStatus.CONVERTED,
                "룰렛 결과",
                "roundNo=1",
                null,
                "roulette-round:10",
                Instant.parse("2026-07-23T00:00:00Z")
        ));

        Mockito.verify(repository).save(Mockito.any(RewardGrant.class));
        Mockito.verifyNoInteractions(validator);
    }

    @Test
    void existsByRouletteRoundId_DelegatesToRepositoryExistsQuery() {
        RewardGrantRepository repository = Mockito.mock(RewardGrantRepository.class);
        RewardPersistenceAdapter adapter = adapter(repository);
        given(repository.existsByRouletteRound_Id(10L)).willReturn(true);

        boolean exists = adapter.existsByRouletteRoundId(10L);

        then(exists).isTrue();
        Mockito.verify(repository).existsByRouletteRound_Id(10L);
    }

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
