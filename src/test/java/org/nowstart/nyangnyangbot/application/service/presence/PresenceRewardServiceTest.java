package org.nowstart.nyangnyangbot.application.service.presence;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase.PresenceApplyCommand;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class PresenceRewardServiceTest {

    @Mock
    private GrantPointUseCase grantPointUseCase;

    @Test
    void applyPresenceReward_UsesPresenceSourceAndStablePerCycleIdempotencyKey() {
        PresenceRewardService service = new PresenceRewardService(grantPointUseCase);
        service.startCapture();
        service.recordChatUser(new ChatReceived(
                "channel-1",
                "user-1",
                new ChatReceived.Profile("치즈냥", null, true),
                "안녕",
                null,
                1L
        ));

        service.applyPresenceReward(new PresenceApplyCommand(List.of("user-1"), 5L));

        ArgumentCaptor<AdjustPointCommand> captor = ArgumentCaptor.forClass(AdjustPointCommand.class);
        verify(grantPointUseCase).grant(captor.capture());
        then(captor.getValue().sourceType()).isEqualTo(PointSourceType.PRESENCE_REWARD);
        then(captor.getValue().idempotencyKey()).startsWith("presence:").endsWith(":user-1");
    }

    @Test
    void applyPresenceReward_RollbackRestoresSameCycleForRetry() {
        PresenceRewardService service = capturedService();
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.applyPresenceReward(new PresenceApplyCommand(List.of("user-1"), 5L));
            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();

            synchronizations.forEach(synchronization -> synchronization.afterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK
            ));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        service.applyPresenceReward(new PresenceApplyCommand(List.of("user-1"), 5L));

        ArgumentCaptor<AdjustPointCommand> captor = ArgumentCaptor.forClass(AdjustPointCommand.class);
        verify(grantPointUseCase, org.mockito.Mockito.times(2)).grant(captor.capture());
        then(captor.getAllValues())
                .extracting(AdjustPointCommand::idempotencyKey)
                .containsExactly(captor.getAllValues().getFirst().idempotencyKey(),
                        captor.getAllValues().getFirst().idempotencyKey());
    }

    @Test
    void applyPresenceReward_WhenAnyDistinctTargetWasNotCapturedRejectsWholeRequest() {
        PresenceRewardService service = capturedService();

        thenThrownBy(() -> service.applyPresenceReward(new PresenceApplyCommand(
                List.of("user-1", "user-1", "missing-user"),
                5L
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("presence targets were not captured: [missing-user]");

        verify(grantPointUseCase, never()).grant(org.mockito.ArgumentMatchers.any());
        then(service.getActiveUsers()).extracting(user -> user.userId()).containsExactly("user-1");
    }

    @Test
    void startCapture_DuringApplyCannotReplaceCycle() throws Exception {
        PresenceRewardService service = capturedService();
        CountDownLatch grantStarted = new CountDownLatch(1);
        CountDownLatch allowGrantToFinish = new CountDownLatch(1);
        willAnswer(invocation -> {
            grantStarted.countDown();
            if (!allowGrantToFinish.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test grant did not resume");
            }
            return null;
        }).given(grantPointUseCase).grant(org.mockito.ArgumentMatchers.any());

        try (var executor = Executors.newSingleThreadExecutor()) {
            CompletableFuture<?> applying = CompletableFuture.runAsync(
                    () -> service.applyPresenceReward(new PresenceApplyCommand(List.of("user-1"), 5L)),
                    executor
            );
            then(grantStarted.await(5, TimeUnit.SECONDS)).isTrue();

            thenThrownBy(service::startCapture)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("presence rewards are being applied");

            allowGrantToFinish.countDown();
            applying.get(5, TimeUnit.SECONDS);
        }

        service.startCapture();
        service.recordChatUser(chat("user-2", "고양이"));
        then(service.getActiveUsers())
                .extracting(user -> user.userId())
                .containsExactly("user-2");
    }

    private PresenceRewardService capturedService() {
        PresenceRewardService service = new PresenceRewardService(grantPointUseCase);
        service.startCapture();
        service.recordChatUser(chat("user-1", "치즈냥"));
        return service;
    }

    private ChatReceived chat(String userId, String displayName) {
        return new ChatReceived(
                "channel-1",
                userId,
                new ChatReceived.Profile(displayName, null, true),
                "안녕",
                null,
                1L
        );
    }
}
