package org.nowstart.nyangnyangbot.domain.roulette;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public class RoulettePolicy {

    public static final int TOTAL_PROBABILITY = 10_000;
    public static final int DEFAULT_HIGH_ROUND_THRESHOLD = 100;

    public RouletteActivationValidation validateActivation(
            TableCandidate table,
            List<? extends ItemCandidate> items
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(table.command())) {
            reasons.add("command is required");
        }
        if (table.pricePerRound() == null || table.pricePerRound() <= 0) {
            reasons.add("pricePerRound is required");
        }
        int probabilityTotal = items.stream()
                .map(ItemCandidate::probabilityBasisPoints)
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

    public <T extends ItemCandidate> T selectItem(List<T> items) {
        return selectItem(items, nextTicket(TOTAL_PROBABILITY));
    }

    public <T extends ItemCandidate> T selectItem(List<T> items, int ticket) {
        int cumulative = 0;
        for (T item : items) {
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
        String normalizedCommand = normalizeCommand(command);
        return Arrays.stream(donationText.trim().split("\\s+"))
                .map(this::normalizeCommand)
                .anyMatch(normalizedCommand::equals);
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

    public int highRoundThreshold(TableCandidate table) {
        return table.highRoundThreshold() == null
                ? DEFAULT_HIGH_ROUND_THRESHOLD
                : table.highRoundThreshold();
    }

    public void validateTableInput(String title, String command, Long pricePerRound) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }
        if (isBlank(command)) {
            throw new IllegalArgumentException("command is required");
        }
        if (pricePerRound == null || pricePerRound <= 0) {
            throw new IllegalArgumentException("pricePerRound is required");
        }
    }

    public void validateItemInput(
            String label,
            Integer probabilityBasisPoints,
            RewardType rewardType,
            ConversionMode conversionMode,
            Integer exchangeFavoriteValue
    ) {
        if (isBlank(label)) {
            throw new IllegalArgumentException("label is required");
        }
        if (probabilityBasisPoints == null || probabilityBasisPoints < 0) {
            throw new IllegalArgumentException("probabilityBasisPoints is required");
        }
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (conversionMode == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (conversionMode == ConversionMode.AUTO
                && (exchangeFavoriteValue == null || exchangeFavoriteValue == 0)) {
            throw new IllegalArgumentException("exchangeFavoriteValue is required for AUTO conversion");
        }
    }

    public int safeRecentRoundLimit(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }

    public int safeSimulationIterations(int iterations) {
        return Math.max(1, Math.min(iterations, 10_000));
    }

    public long parseDonationAmount(String amount) {
        if (isBlank(amount)) {
            return 0L;
        }
        String digits = amount.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    public RouletteEventStatus eventStatus(List<? extends RoundStatusCandidate> rounds) {
        long applied = rounds.stream()
                .filter(round -> round.status() == RouletteRoundStatus.APPLIED)
                .count();
        long failed = rounds.stream()
                .filter(round -> round.status() == RouletteRoundStatus.FAILED)
                .count();
        if (applied == rounds.size()) {
            return RouletteEventStatus.APPLIED;
        }
        if (failed == rounds.size()) {
            return RouletteEventStatus.FAILED;
        }
        if (applied > 0 || failed > 0) {
            return RouletteEventStatus.PARTIALLY_APPLIED;
        }
        return RouletteEventStatus.CONFIRMED;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeCommand(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
    }

    public interface TableCandidate {
        String command();

        Long pricePerRound();

        Integer highRoundThreshold();
    }

    public interface ItemCandidate {
        Integer probabilityBasisPoints();

        boolean losingItem();
    }

    public interface RoundStatusCandidate {
        RouletteRoundStatus status();
    }
}
