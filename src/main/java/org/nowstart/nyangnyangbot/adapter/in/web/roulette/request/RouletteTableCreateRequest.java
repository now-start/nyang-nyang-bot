package org.nowstart.nyangnyangbot.adapter.in.web.roulette.request;

public record RouletteTableCreateRequest(
        String title,
        String command,
        Long pricePerRound,
        Integer highRoundThreshold
) {
}
