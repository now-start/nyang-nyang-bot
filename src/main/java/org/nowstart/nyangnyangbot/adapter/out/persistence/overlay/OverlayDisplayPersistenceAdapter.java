package org.nowstart.nyangnyangbot.adapter.out.persistence.overlay;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.entity.OverlayDisplayJob;
import org.nowstart.nyangnyangbot.adapter.out.persistence.overlay.repository.OverlayDisplayJobRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.entity.RouletteRun;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRoundRepository.DisplayRoundProjection;
import org.nowstart.nyangnyangbot.adapter.out.persistence.roulette.repository.RouletteRunRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.domain.type.OverlayDisplayStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OverlayDisplayPersistenceAdapter implements OverlayDisplayPort {

    private final OverlayDisplayJobRepository overlayDisplayJobRepository;
    private final RouletteRoundRepository rouletteRoundRepository;
    private final RouletteRunRepository rouletteRunRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    @Transactional
    public void enqueue(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    ) {
        RouletteRun run = requireRunForUpdate(rouletteRunId);
        if (overlayDisplayJobRepository.findByIdempotencyKey(idempotencyKey).isEmpty()) {
            createJob(run, null, idempotencyKey, expiresAt, createdAt);
        }
    }

    @Override
    @Transactional
    public Long replay(
            Long rouletteRunId,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    ) {
        RouletteRun run = requireRunForUpdate(rouletteRunId);
        OverlayDisplayJob replayOf = overlayDisplayJobRepository
                .findFirstByRouletteRun_DonationIdOrderByCreatedAtDescIdDesc(rouletteRunId)
                .orElseThrow(() -> new IllegalArgumentException("overlay display job not found"));
        return createJob(run, replayOf, idempotencyKey, expiresAt, createdAt);
    }

    @Override
    @Transactional
    public void markExpiredMissed(Instant current) {
        overlayDisplayJobRepository.markExpiredMissed(
                current,
                OverlayDisplayStatus.MISSED,
                List.of(OverlayDisplayStatus.PENDING, OverlayDisplayStatus.DISPLAYING)
        );
    }

    @Override
    @Transactional
    public Optional<DisplayJobResult> claimNext(Instant current, String claimToken, Instant claimExpiresAt) {
        return overlayDisplayJobRepository.findClaimableForUpdate(
                        current,
                        OverlayDisplayStatus.PENDING,
                        OverlayDisplayStatus.DISPLAYING,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(job -> {
                    job.claim(claimToken, current, claimExpiresAt);
                    overlayDisplayJobRepository.flush();
                    return displayJobResult(job);
                });
    }

    @Override
    @Transactional
    public void markDisplayed(Long displayJobId, String claimToken, Instant displayedAt) {
        OverlayDisplayJob job = overlayDisplayJobRepository.findByIdForUpdate(displayJobId)
                .orElseThrow(() -> new IllegalArgumentException("overlay display job not found"));
        job.markDisplayed(claimToken, displayedAt);
    }

    private Long createJob(
            RouletteRun run,
            OverlayDisplayJob replayOf,
            String idempotencyKey,
            Instant expiresAt,
            Instant createdAt
    ) {
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("overlay display expiry must be after creation");
        }
        OverlayDisplayJob saved = overlayDisplayJobRepository.save(OverlayDisplayJob.builder()
                .rouletteRun(run)
                .replayOfJob(replayOf)
                .idempotencyKey(idempotencyKey)
                .status(OverlayDisplayStatus.PENDING)
                .expiresAt(expiresAt)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
        return saved.getId();
    }

    private RouletteRun requireRunForUpdate(Long rouletteRunId) {
        return rouletteRunRepository.findByIdForUpdate(rouletteRunId)
                .orElseThrow(() -> new IllegalArgumentException("roulette run not found"));
    }

    private DisplayJobResult displayJobResult(OverlayDisplayJob job) {
        RouletteRun run = job.getRouletteRun();
        long roundCount = rouletteRoundRepository.countByRouletteRun_DonationId(run.getDonationId());
        List<DisplayRoundResult> rounds = rouletteRoundRepository
                .findDisplayRoundsByRunId(run.getDonationId(), PageRequest.of(0, MAX_DISPLAY_ROUNDS))
                .stream()
                .map(this::roundResult)
                .toList();
        return contractValidator.persistenceResult("overlay.displayJob", new DisplayJobResult(
                job.getId(),
                run.getDonation().getDonorDisplayName(),
                job.getClaimToken(),
                roundCount,
                rounds
        ));
    }

    private DisplayRoundResult roundResult(DisplayRoundProjection round) {
        return contractValidator.persistenceResult("overlay.displayRound", new DisplayRoundResult(
                round.getId(),
                round.getRoundNo(),
                round.getOptionLabel(),
                round.getLosing(),
                round.getRewardType(),
                round.getConversionMode(),
                round.getPointDelta(),
                round.getStatus(),
                round.getFailureReason()
        ));
    }
}
