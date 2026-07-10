package org.nowstart.nyangnyangbot.application.port.in.roulette;

import java.util.List;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.HandleChzzkEventUseCase.DonationReceived;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;

public interface ProcessRouletteDonationUseCase {

    RouletteRunResult processDonation(DonationReceived donation);

    record RouletteRunResult(
            Status status,
            Long eventId,
            String reason,
            Integer roundCount,
            List<RouletteRoundResult> rounds
    ) {

        public static RouletteRunResult ignored(String reason) {
            return new RouletteRunResult(Status.IGNORED, null, reason, 0, List.of());
        }

        public static RouletteRunResult duplicate() {
            return new RouletteRunResult(Status.DUPLICATE, null, "duplicate donation event", 0, List.of());
        }

        public static RouletteRunResult confirmed(Long eventId, Integer roundCount, List<RouletteRoundResult> rounds) {
            return new RouletteRunResult(Status.CONFIRMED, eventId, null, roundCount, rounds);
        }

        public enum Status {
            CONFIRMED,
            IGNORED,
            DUPLICATE
        }
    }
}
