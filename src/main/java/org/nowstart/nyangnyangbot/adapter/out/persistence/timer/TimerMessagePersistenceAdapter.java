package org.nowstart.nyangnyangbot.adapter.out.persistence.timer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.timer.TimerMessagePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TimerMessagePersistenceAdapter implements TimerMessagePort {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final TimerMessageRepository timerMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    public List<TimerMessageRecord> findAllOrderByIdDesc() {
        return timerMessageRepository.findAllByOrderByIdDesc().stream()
                .map(this::record)
                .toList();
    }

    @Override
    public Optional<TimerMessageRecord> findByIdForUpdate(Long timerMessageId) {
        return timerMessageRepository.findByIdForUpdate(timerMessageId).map(this::record);
    }

    @Override
    public TimerMessageRecord create(CreateData data) {
        contractValidator.request("timerMessage.create", data);
        TimerMessage saved = timerMessageRepository.save(TimerMessage.builder()
                .messageTemplate(data.messageTemplate())
                .intervalMinutes(data.intervalMinutes())
                .minChatCount(data.minChatCount())
                .active(data.active())
                .nextRunAt(data.nextRunAt())
                .chatCountSinceLastSend(0)
                .claimedChatCount(0)
                .createdByUser(actor(data.createdBy()))
                .updatedByUser(actor(data.updatedBy()))
                .build());
        return record(saved);
    }

    @Override
    public TimerMessageRecord update(UpdateData data) {
        contractValidator.request("timerMessage.update", data);
        TimerMessage timer = timerMessageRepository.findByIdForUpdate(data.id())
                .orElseThrow(() -> new IllegalArgumentException("timer message not found"));
        timer.update(
                data.messageTemplate(),
                data.intervalMinutes(),
                data.minChatCount(),
                data.active(),
                data.nextRunAt(),
                data.resetSchedule(),
                actor(data.updatedBy())
        );
        return record(timer);
    }

    @Override
    public void incrementActiveChatCounts() {
        timerMessageRepository.incrementActiveChatCounts();
    }

    @Override
    public List<Long> findClaimCandidateIds(LocalDateTime now, int limit) {
        return timerMessageRepository.findClaimCandidateIds(now, PageRequest.of(0, limit));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedTimerMessage> claimDue(
            Long timerMessageId,
            String claimToken,
            LocalDateTime now,
            LocalDateTime claimExpiresAt
    ) {
        int claimed = timerMessageRepository.claimDue(timerMessageId, claimToken, now, claimExpiresAt);
        if (claimed != 1) {
            return Optional.empty();
        }
        return timerMessageRepository.findByIdAndClaimToken(timerMessageId, claimToken)
                .map(timer -> new ClaimedTimerMessage(
                        timer.getId(),
                        timer.getMessageTemplate(),
                        timer.getIntervalMinutes(),
                        timer.getNextRunAt(),
                        timer.getClaimToken()
                ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeClaim(
            Long timerMessageId,
            String claimToken,
            LocalDateTime claimedNextRunAt,
            Integer claimedIntervalMinutes,
            LocalDateTime sentAt,
            LocalDateTime nextRunAt
    ) {
        return timerMessageRepository.completeClaim(
                timerMessageId,
                claimToken,
                claimedNextRunAt,
                claimedIntervalMinutes,
                sentAt,
                nextRunAt
        ) == 1;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean releaseClaim(
            Long timerMessageId,
            String claimToken,
            LocalDateTime claimedNextRunAt,
            Integer claimedIntervalMinutes,
            LocalDateTime retryAt
    ) {
        return timerMessageRepository.releaseClaim(
                timerMessageId,
                claimToken,
                claimedNextRunAt,
                claimedIntervalMinutes,
                retryAt
        ) == 1;
    }

    private TimerMessageRecord record(TimerMessage timer) {
        return contractValidator.persistenceResult("timerMessage.record", new TimerMessageRecord(
                timer.getId(),
                timer.getMessageTemplate(),
                timer.getIntervalMinutes(),
                timer.getMinChatCount(),
                timer.isActive(),
                timer.getChatCountSinceLastSend(),
                timer.getLastSentAt(),
                timer.getNextRunAt(),
                userId(timer.getCreatedByUser()),
                userId(timer.getUpdatedByUser()),
                localDateTime(timer.getCreatedAt()),
                localDateTime(timer.getUpdatedAt())
        ));
    }

    private UserAccount actor(String userId) {
        if (userId == null || userId.isBlank() || "system".equals(userId)) {
            return null;
        }
        return userAccountRepository.getReferenceById(userId);
    }

    private String userId(UserAccount user) {
        return user == null ? null : user.getUserId();
    }

    private LocalDateTime localDateTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, SEOUL);
    }
}
