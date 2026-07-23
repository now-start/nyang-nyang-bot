package org.nowstart.nyangnyangbot.application.service.roulette;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService;
import org.nowstart.nyangnyangbot.application.service.reward.RewardService.RouletteRewardCommand;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouletteRoundApplyService {

    private final RoulettePort roulettePort;
    private final RewardService rewardService;

    @Transactional
    public void applyRound(Long roundId) {
        RoundResult round = roulettePort.findRoundByIdForUpdate(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        if (round.status() != RouletteRoundStatus.CONFIRMED) {
            return;
        }
        if (!round.losing()) {
            rewardService.grantRoulette(new RouletteRewardCommand(
                    round.id(),
                    round.userId(),
                    round.donorDisplayName(),
                    round.ingestionKey(),
                    round.optionLabel(),
                    round.rewardType(),
                    round.conversionMode(),
                    round.pointDelta(),
                    "룰렛 결과: " + round.optionLabel(),
                    "ingestionKey=" + round.ingestionKey() + " roundNo=" + round.roundNo()
            ));
        }
        roulettePort.markRoundApplied(round.id(), now());
    }

    @Transactional
    public void failRound(Long roundId, String failureReason) {
        RoundResult round = roulettePort.findRoundByIdForUpdate(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        if (round.status() == RouletteRoundStatus.CONFIRMED) {
            roulettePort.markRoundFailed(roundId, normalizeFailureReason(failureReason), now());
        }
    }

    Instant now() {
        return Instant.now();
    }

    private String normalizeFailureReason(String failureReason) {
        String normalized = failureReason == null || failureReason.isBlank()
                ? "reward processing failed"
                : failureReason.trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }
}
