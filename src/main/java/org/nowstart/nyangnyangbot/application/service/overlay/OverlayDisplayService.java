package org.nowstart.nyangnyangbot.application.service.overlay;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.ManageOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayJobResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayDisplayPort.DisplayRoundResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OverlayDisplayService implements ManageOverlayDisplayUseCase, QueueOverlayDisplayUseCase {

    private static final int DISPLAY_TTL_SECONDS = 120;
    private static final int CLAIM_LEASE_SECONDS = 30;

    private final OverlayTokenService overlayTokenService;
    private final OverlayDisplayPort overlayDisplayPort;

    @Override
    @Transactional
    public void enqueueRouletteRun(Long rouletteRunId) {
        Instant current = now();
        overlayDisplayPort.enqueue(
                rouletteRunId,
                "roulette-run:" + rouletteRunId,
                current.plusSeconds(DISPLAY_TTL_SECONDS),
                current
        );
    }

    @Override
    @Transactional
    public void replayRouletteRun(Long rouletteRunId) {
        Instant current = now();
        Long displayJobId = overlayDisplayPort.replay(
                rouletteRunId,
                "roulette-run:" + rouletteRunId + ":replay:" + newClaimToken(),
                current.plusSeconds(DISPLAY_TTL_SECONDS),
                current
        );
        log.info("level=AUDIT action=overlay.replay result=success rouletteRunId={} displayJobId={}",
                rouletteRunId, displayJobId);
    }

    @Override
    @Transactional
    public Optional<OverlayDisplayResult> claimNextJob(String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        Instant current = now();
        overlayDisplayPort.markExpiredMissed(current);
        return overlayDisplayPort.claimNext(
                        current,
                        newClaimToken(),
                        current.plusSeconds(CLAIM_LEASE_SECONDS)
                )
                .map(this::overlayDisplayResult);
    }

    @Override
    @Transactional
    public void markDisplayed(Long displayJobId, String claimToken, String authorizationHeader) {
        validateAuthorization(authorizationHeader);
        if (claimToken == null || claimToken.isBlank()) {
            throw new IllegalArgumentException("overlay claim token is required");
        }
        overlayDisplayPort.markDisplayed(displayJobId, claimToken, now());
    }

    OverlayDisplayResult overlayDisplayResult(DisplayJobResult job) {
        return new OverlayDisplayResult(
                job.id(),
                job.donorDisplayName(),
                job.claimToken(),
                Math.toIntExact(job.roundCount()),
                job.rounds().stream().map(this::rouletteRoundResult).toList()
        );
    }

    private RouletteRoundResult rouletteRoundResult(DisplayRoundResult round) {
        return new RouletteRoundResult(
                round.id(),
                round.roundNo(),
                round.optionLabel(),
                round.losing(),
                round.rewardType().name(),
                round.conversionMode().name(),
                round.pointDelta(),
                round.status().name(),
                round.failureReason()
        );
    }

    Instant now() {
        return Instant.now();
    }

    String newClaimToken() {
        return UUID.randomUUID().toString();
    }

    private void validateAuthorization(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (!overlayTokenService.validateToken(token)) {
            throw new IllegalArgumentException("invalid overlay token");
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring("Bearer ".length()).trim();
    }
}
