package org.nowstart.nyangnyangbot.adapter.out.persistence.timer;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.entity.TimerMessage;
import org.nowstart.nyangnyangbot.adapter.out.persistence.timer.repository.TimerMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("local")
class TimerMessagePersistenceConcurrencyTest {

    @Autowired
    private TimerMessageRepository timerMessageRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void adminEditFlushedAfterClaimCompletion_ShouldNotRestoreStaleClaimState() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 16, 21, 0);
        Long timerMessageId = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.saveAndFlush(TimerMessage.builder()
                        .messageTemplate("이전 메시지")
                        .intervalMinutes(30)
                        .minChatCount(10)
                        .active(true)
                        .nextRunAt(now)
                        .chatCountSinceLastSend(12)
                        .claimedChatCount(10)
                        .claimToken("claim-1")
                        .claimExpiresAt(now.plusMinutes(2))
                        .createdBy("admin")
                        .updatedBy("admin")
                        .build()).getId()
        );

        TransactionTemplate inner = new TransactionTemplate(transactionManager);
        inner.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            TimerMessage staleAdminView = timerMessageRepository.findById(timerMessageId).orElseThrow();

            inner.executeWithoutResult(innerStatus -> then(timerMessageRepository.completeClaim(
                    timerMessageId,
                    "claim-1",
                    now,
                    30,
                    now,
                    now.plusMinutes(30)
            )).isEqualTo(1));

            staleAdminView.update(
                    "수정한 메시지",
                    30,
                    10,
                    true,
                    now,
                    false,
                    "editor"
            );
        });

        TimerMessage saved = timerMessageRepository.findById(timerMessageId).orElseThrow();
        then(saved.getMessageTemplate()).isEqualTo("수정한 메시지");
        then(saved.getClaimToken()).isNull();
        then(saved.getClaimExpiresAt()).isNull();
        then(saved.getClaimedChatCount()).isZero();
        then(saved.getChatCountSinceLastSend()).isEqualTo(2);
        then(saved.getLastSentAt()).isEqualTo(now);
        then(saved.getNextRunAt()).isEqualTo(now.plusMinutes(30));
    }

    @Test
    void completeClaim_AfterAdminDeactivation_ShouldKeepTimerUnscheduled() {
        LocalDateTime claimedNextRunAt = LocalDateTime.of(2026, 7, 16, 21, 0);
        Long timerMessageId = saveClaimedTimer(
                false,
                30,
                null,
                claimedNextRunAt,
                "claim-deactivated",
                0,
                10
        );

        int completed = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.completeClaim(
                        timerMessageId,
                        "claim-deactivated",
                        claimedNextRunAt,
                        30,
                        claimedNextRunAt.plusMinutes(1),
                        claimedNextRunAt.plusMinutes(31)
                )
        );

        then(completed).isEqualTo(1);
        TimerMessage saved = timerMessageRepository.findById(timerMessageId).orElseThrow();
        then(saved.isActive()).isFalse();
        then(saved.getNextRunAt()).isNull();
        then(saved.getClaimToken()).isNull();
    }

    @Test
    void completeClaim_AfterAdminIntervalChange_ShouldKeepAdminSchedule() {
        LocalDateTime claimedNextRunAt = LocalDateTime.of(2026, 7, 16, 21, 0);
        LocalDateTime adminNextRunAt = claimedNextRunAt.plusMinutes(60);
        Long timerMessageId = saveClaimedTimer(
                true,
                30,
                claimedNextRunAt,
                claimedNextRunAt,
                "claim-interval-change",
                12,
                10
        );
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            TimerMessage timer = timerMessageRepository.findById(timerMessageId).orElseThrow();
            timer.update(
                    "관리자가 수정한 메시지",
                    60,
                    10,
                    true,
                    adminNextRunAt,
                    true,
                    "editor"
            );
            timerMessageRepository.flush();
        });
        for (int i = 0; i < 5; i++) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                    timerMessageRepository.incrementActiveChatCounts()
            );
        }

        int completed = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.completeClaim(
                        timerMessageId,
                        "claim-interval-change",
                        claimedNextRunAt,
                        30,
                        claimedNextRunAt.plusMinutes(1),
                        claimedNextRunAt.plusMinutes(31)
                )
        );

        then(completed).isEqualTo(1);
        TimerMessage saved = timerMessageRepository.findById(timerMessageId).orElseThrow();
        then(saved.getIntervalMinutes()).isEqualTo(60);
        then(saved.getNextRunAt()).isEqualTo(adminNextRunAt);
        then(saved.getChatCountSinceLastSend()).isEqualTo(5);
        then(saved.getClaimToken()).isNull();
    }

    @Test
    void releaseClaim_AfterAdminDeactivation_ShouldKeepTimerUnscheduled() {
        LocalDateTime claimedNextRunAt = LocalDateTime.of(2026, 7, 16, 21, 0);
        Long timerMessageId = saveClaimedTimer(
                false,
                30,
                null,
                claimedNextRunAt,
                "claim-release",
                0,
                10
        );

        int released = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.releaseClaim(
                        timerMessageId,
                        "claim-release",
                        claimedNextRunAt,
                        30,
                        claimedNextRunAt.plusMinutes(1)
                )
        );

        then(released).isEqualTo(1);
        TimerMessage saved = timerMessageRepository.findById(timerMessageId).orElseThrow();
        then(saved.getNextRunAt()).isNull();
        then(saved.getClaimToken()).isNull();
    }

    @Test
    void claimAndComplete_ShouldAllowSingleClaimAndPreserveChatsArrivingInFlight() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 16, 21, 0);
        Long timerMessageId = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.saveAndFlush(TimerMessage.builder()
                        .messageTemplate("채팅 보존")
                        .intervalMinutes(30)
                        .minChatCount(10)
                        .active(true)
                        .nextRunAt(now)
                        .chatCountSinceLastSend(10)
                        .claimedChatCount(0)
                        .createdBy("admin")
                        .updatedBy("admin")
                        .build()).getId()
        );

        int firstClaim = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.claimDue(
                        timerMessageId,
                        "claim-first",
                        now,
                        now.plusMinutes(2)
                )
        );
        int secondClaim = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.claimDue(
                        timerMessageId,
                        "claim-second",
                        now,
                        now.plusMinutes(2)
                )
        );
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                timerMessageRepository.incrementActiveChatCounts()
        );
        int completed = new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.completeClaim(
                        timerMessageId,
                        "claim-first",
                        now,
                        30,
                        now.plusMinutes(1),
                        now.plusMinutes(31)
                )
        );

        then(firstClaim).isEqualTo(1);
        then(secondClaim).isZero();
        then(completed).isEqualTo(1);
        TimerMessage saved = timerMessageRepository.findById(timerMessageId).orElseThrow();
        then(saved.getChatCountSinceLastSend()).isEqualTo(1);
        then(saved.getClaimedChatCount()).isZero();
        then(saved.getClaimToken()).isNull();
    }

    private Long saveClaimedTimer(
            boolean active,
            int intervalMinutes,
            LocalDateTime nextRunAt,
            LocalDateTime claimTime,
            String claimToken,
            long chatCountSinceLastSend,
            long claimedChatCount
    ) {
        return new TransactionTemplate(transactionManager).execute(status ->
                timerMessageRepository.saveAndFlush(TimerMessage.builder()
                        .messageTemplate("설정 변경 중 발송")
                        .intervalMinutes(intervalMinutes)
                        .minChatCount(10)
                        .active(active)
                        .nextRunAt(nextRunAt)
                        .chatCountSinceLastSend(chatCountSinceLastSend)
                        .claimedChatCount(claimedChatCount)
                        .claimToken(claimToken)
                        .claimExpiresAt(claimTime.plusMinutes(2))
                        .createdBy("admin")
                        .updatedBy("admin")
                        .build()).getId()
        );
    }
}
