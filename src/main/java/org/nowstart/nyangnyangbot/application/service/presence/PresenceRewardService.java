package org.nowstart.nyangnyangbot.application.service.presence;

import io.micrometer.common.util.StringUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.ChatReceived;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.GrantPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.ManagePresenceRewardUseCase;
import org.nowstart.nyangnyangbot.application.port.in.presence.RecordPresenceChatUseCase;
import org.nowstart.nyangnyangbot.application.service.chat.ChatEventSupport;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.nowstart.nyangnyangbot.domain.presence.PresenceUserState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PresenceRewardService implements ManagePresenceRewardUseCase, RecordPresenceChatUseCase {

    private final GrantPointUseCase grantPointUseCase;
    private final ReentrantLock cycleLock = new ReentrantLock();
    private PresenceCycle cycle = PresenceCycle.inactive();

    @Override
    public void startCapture() {
        cycleLock.lock();
        try {
            requireNotApplying();
            cycle = PresenceCycle.start();
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public void stopCapture() {
        cycleLock.lock();
        try {
            requireNotApplying();
            cycle = PresenceCycle.inactive();
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public void recordChatUser(ChatReceived chat) {
        if (!ChatEventSupport.hasSenderChannelId(chat)) {
            return;
        }
        String userId = ChatEventSupport.senderChannelId(chat);
        String displayName = ChatEventSupport.displayName(chat);
        long now = System.currentTimeMillis();
        cycleLock.lock();
        try {
            if (cycle.status() != CycleStatus.ACTIVE) {
                return;
            }
            cycle.users().compute(userId, (key, existing) -> new PresenceUserState(
                    userId,
                    StringUtils.isBlank(displayName) && existing != null ? existing.displayName() : displayName,
                    now
            ));
        } finally {
            cycleLock.unlock();
        }
    }

    @Override
    public List<PresenceUserSnapshot> getActiveUsers() {
        List<PresenceUserState> users;
        cycleLock.lock();
        try {
            users = List.copyOf(cycle.users().values());
        } finally {
            cycleLock.unlock();
        }
        return users.stream()
                .sorted(Comparator.comparingLong(PresenceUserState::lastMessageTime).reversed())
                .map(user -> new PresenceUserSnapshot(
                        user.userId(),
                        user.displayName(),
                        user.lastMessageTime()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void applyPresenceReward(PresenceApplyCommand command) {
        ApplyingCycle applying = beginApply(command.userIds());
        boolean synchronizedCompletion = registerTransactionCompletion(applying.cycleId());
        try {
            for (PresenceUserState target : applying.users()) {
                grantPointUseCase.grant(AdjustPointCommand.builder()
                        .userId(target.userId())
                        .displayName(target.displayName())
                        .delta(command.amount())
                        .sourceType(PointSourceType.PRESENCE_REWARD)
                        .sourceReference(applying.cycleId())
                        .description(String.format(Locale.ROOT, "생존 확인 보상(+%d)", command.amount()))
                        .idempotencyKey("presence:" + applying.cycleId() + ":" + target.userId())
                        .allowNegativeBalance(false)
                        .createIfMissing(true)
                        .build());
            }
            if (!synchronizedCompletion) {
                completeApply(applying.cycleId(), true);
            }
        } catch (RuntimeException | Error failure) {
            if (!synchronizedCompletion) {
                completeApply(applying.cycleId(), false);
            }
            throw failure;
        }
    }

    private ApplyingCycle beginApply(List<String> userIds) {
        cycleLock.lock();
        try {
            if (cycle.status() != CycleStatus.ACTIVE) {
                throw new IllegalStateException("presence cycle is not active");
            }
            List<String> distinctUserIds = userIds.stream().distinct().toList();
            if (distinctUserIds.isEmpty()) {
                throw new IllegalArgumentException("presence targets are required");
            }
            List<String> missingUserIds = distinctUserIds.stream()
                    .filter(userId -> !cycle.users().containsKey(userId))
                    .toList();
            if (!missingUserIds.isEmpty()) {
                throw new IllegalArgumentException("presence targets were not captured: " + missingUserIds);
            }
            List<PresenceUserState> targets = distinctUserIds.stream()
                    .map(cycle.users()::get)
                    .toList();
            String cycleId = cycle.cycleId();
            cycle = cycle.applying();
            return new ApplyingCycle(cycleId, targets);
        } finally {
            cycleLock.unlock();
        }
    }

    private boolean registerTransactionCompletion(String cycleId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    completeApply(cycleId, status == STATUS_COMMITTED);
                }
            });
            return true;
        } catch (RuntimeException | Error failure) {
            completeApply(cycleId, false);
            throw failure;
        }
    }

    private void completeApply(String cycleId, boolean committed) {
        cycleLock.lock();
        try {
            if (cycle.status() != CycleStatus.APPLYING || !cycle.cycleId().equals(cycleId)) {
                return;
            }
            cycle = committed ? PresenceCycle.inactive() : cycle.activeAgain();
        } finally {
            cycleLock.unlock();
        }
    }

    private void requireNotApplying() {
        if (cycle.status() == CycleStatus.APPLYING) {
            throw new IllegalStateException("presence rewards are being applied");
        }
    }

    private enum CycleStatus {
        INACTIVE,
        ACTIVE,
        APPLYING
    }

    private record PresenceCycle(CycleStatus status, String cycleId, Map<String, PresenceUserState> users) {
        static PresenceCycle start() {
            return new PresenceCycle(CycleStatus.ACTIVE, UUID.randomUUID().toString(), new HashMap<>());
        }

        static PresenceCycle inactive() {
            return new PresenceCycle(CycleStatus.INACTIVE, null, Map.of());
        }

        PresenceCycle applying() {
            return new PresenceCycle(CycleStatus.APPLYING, cycleId, users);
        }

        PresenceCycle activeAgain() {
            return new PresenceCycle(CycleStatus.ACTIVE, cycleId, users);
        }
    }

    private record ApplyingCycle(String cycleId, List<PresenceUserState> users) {
    }
}
