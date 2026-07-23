package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ProcessRouletteDonationUseCase {

    RouletteRunResult processDonation(Long donationId, DonationReceived donation);

    record RouletteRunResult(
            Status status,
            Long runId,
            String reason,
            Integer roundCount,
            List<RouletteRoundResult> rounds
    ) {

        public static RouletteRunResult ignored(String reason) {
            return new RouletteRunResult(Status.IGNORED, null, reason, 0, List.of());
        }

        public static RouletteRunResult duplicate(Long runId, List<RouletteRoundResult> rounds) {
            return new RouletteRunResult(Status.DUPLICATE, runId, "duplicate donation event", rounds.size(), rounds);
        }

        public static RouletteRunResult ready(Long runId, List<RouletteRoundResult> rounds) {
            return new RouletteRunResult(Status.READY, runId, null, rounds.size(), rounds);
        }

        public enum Status {
            READY,
            IGNORED,
            DUPLICATE
        }
    }
}
