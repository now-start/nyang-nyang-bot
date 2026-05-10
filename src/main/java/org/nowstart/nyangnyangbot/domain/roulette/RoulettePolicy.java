package org.nowstart.nyangnyangbot.domain.roulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.nowstart.nyangnyangbot.domain.model.RouletteItem;
import org.nowstart.nyangnyangbot.domain.model.RouletteTable;

public class RoulettePolicy {

    public static final int TOTAL_PROBABILITY = 10_000;
    public static final int DEFAULT_HIGH_ROUND_THRESHOLD = 100;

    public RouletteActivationValidation validateActivation(
            RouletteTable table,
            List<RouletteItem> items
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(table.command())) {
            reasons.add("command is required");
        }
        if (table.pricePerRound() == null || table.pricePerRound() <= 0) {
            reasons.add("pricePerRound is required");
        }
        int probabilityTotal = items.stream()
                .map(RouletteItem::probabilityBasisPoints)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (probabilityTotal != TOTAL_PROBABILITY) {
            reasons.add("probability total must be 10000");
        }
        boolean hasLosingItem = items.stream().anyMatch(item ->
                item.losingItem()
                        && item.probabilityBasisPoints() != null
                        && item.probabilityBasisPoints() > 0
        );
        if (!hasLosingItem) {
            reasons.add("losing item is required");
        }
        return new RouletteActivationValidation(
                reasons.isEmpty(),
                reasons,
                probabilityTotal,
                hasLosingItem
        );
    }

    public RouletteItem selectItem(List<RouletteItem> items) {
        return selectItem(items, nextTicket(TOTAL_PROBABILITY));
    }

    public RouletteItem selectItem(List<RouletteItem> items, int ticket) {
        int cumulative = 0;
        for (RouletteItem item : items) {
            cumulative += item.probabilityBasisPoints();
            if (ticket <= cumulative) {
                return item;
            }
        }
        return items.get(items.size() - 1);
    }

    public int nextTicket(int totalProbability) {
        return ThreadLocalRandom.current().nextInt(1, totalProbability + 1);
    }

    public boolean containsCommand(String donationText, String command) {
        if (isBlank(donationText) || isBlank(command)) {
            return false;
        }
        return Arrays.stream(donationText.trim().split("\\s+"))
                .anyMatch(command::equals);
    }

    public int calculateRoundCount(long amount, Long pricePerRound) {
        if (pricePerRound == null || pricePerRound <= 0) {
            return 0;
        }
        long roundCount = amount / pricePerRound;
        if (roundCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("roundCount is too large");
        }
        return (int) roundCount;
    }

    public int highRoundThreshold(RouletteTable table) {
        return table.highRoundThreshold() == null
                ? DEFAULT_HIGH_ROUND_THRESHOLD
                : table.highRoundThreshold();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
