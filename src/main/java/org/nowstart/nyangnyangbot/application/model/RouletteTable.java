package org.nowstart.nyangnyangbot.application.model;

public record RouletteTable(
        Long id,
        String title,
        String command,
        Long pricePerRound,
        boolean active,
        Integer version,
        Integer highRoundThreshold
) {
}
