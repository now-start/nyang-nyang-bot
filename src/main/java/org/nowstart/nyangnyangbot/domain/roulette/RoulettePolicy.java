package org.nowstart.nyangnyangbot.domain.roulette;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.nowstart.nyangnyangbot.domain.chat.CommandTrigger;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RewardType;
import org.nowstart.nyangnyangbot.domain.type.RouletteProcessingStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;

public class RoulettePolicy {

    public static final int TOTAL_PROBABILITY = 10_000;
    public static final int DEFAULT_HIGH_ROUND_THRESHOLD = 100;
    public static final int MAX_ROUNDS_PER_DONATION = 1_000;
    public static final int MIN_SIMULATION_ITERATIONS = 1;
    public static final int DEFAULT_SIMULATION_ITERATIONS = 10_000;
    public static final int MAX_SIMULATION_ITERATIONS = 10_000;
    public static final int MAX_FAILURE_REASON_LENGTH = 500;

    public RouletteActivationValidation validateActivation(
            ConfigCandidate config,
            List<? extends OptionCandidate> options
    ) {
        List<String> reasons = new ArrayList<>();
        if (isBlank(config.triggerToken())) {
            reasons.add("triggerToken is required");
        }
        if (config.pricePerRound() == null || config.pricePerRound() <= 0) {
            reasons.add("pricePerRound is required");
        }
        int probabilityTotal = options.stream()
                .map(OptionCandidate::probabilityBasisPoints)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();
        if (probabilityTotal != TOTAL_PROBABILITY) {
            reasons.add("probability total must be 10000");
        }
        boolean hasLosingOption = options.stream().anyMatch(option ->
                option.losing()
                        && option.probabilityBasisPoints() != null
                        && option.probabilityBasisPoints() > 0
        );
        if (!hasLosingOption) {
            reasons.add("losing option is required");
        }
        return new RouletteActivationValidation(
                reasons.isEmpty(),
                reasons,
                probabilityTotal,
                hasLosingOption
        );
    }

    public <T extends OptionCandidate> T selectOption(List<T> options) {
        return selectOption(options, nextTicket(TOTAL_PROBABILITY));
    }

    public <T extends OptionCandidate> T selectOption(List<T> options, int ticket) {
        int cumulative = 0;
        for (T option : options) {
            cumulative += option.probabilityBasisPoints();
            if (ticket <= cumulative) {
                return option;
            }
        }
        return options.getLast();
    }

    public int nextTicket(int totalProbability) {
        return ThreadLocalRandom.current().nextInt(1, totalProbability + 1);
    }

    public boolean containsTriggerToken(String donationText, String triggerToken) {
        if (isBlank(donationText) || isBlank(triggerToken)) {
            return false;
        }
        String normalizedCommand = CommandTrigger.normalize(triggerToken);
        return Arrays.stream(donationText.trim().split("\\s+"))
                .map(CommandTrigger::normalize)
                .anyMatch(normalizedCommand::equals);
    }

    public int calculateRoundCount(long amount, Long pricePerRound) {
        if (pricePerRound == null || pricePerRound <= 0) {
            return 0;
        }
        long roundCount = amount / pricePerRound;
        if (roundCount > MAX_ROUNDS_PER_DONATION) {
            throw new IllegalArgumentException("roundCount exceeds maximum " + MAX_ROUNDS_PER_DONATION);
        }
        return (int) roundCount;
    }

    public int highRoundThreshold(ConfigCandidate config) {
        return config.highRoundThreshold() == null
                ? DEFAULT_HIGH_ROUND_THRESHOLD
                : config.highRoundThreshold();
    }

    public void validateConfigInput(String title, String triggerToken, Long pricePerRound) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("title is required");
        }
        if (isBlank(triggerToken)) {
            throw new IllegalArgumentException("triggerToken is required");
        }
        String trimmedTrigger = triggerToken.trim();
        CommandTrigger.validate(trimmedTrigger);
        if (!trimmedTrigger.startsWith("!")) {
            throw new IllegalArgumentException("triggerToken must start with !");
        }
        if (pricePerRound == null || pricePerRound <= 0) {
            throw new IllegalArgumentException("pricePerRound is required");
        }
    }

    public void validateOptionInput(
            String label,
            Integer probabilityBasisPoints,
            boolean losing,
            RewardType rewardType,
            ConversionMode conversionMode,
            Long pointDelta
    ) {
        if (isBlank(label)) {
            throw new IllegalArgumentException("label is required");
        }
        if (probabilityBasisPoints == null
                || probabilityBasisPoints < 0
                || probabilityBasisPoints > TOTAL_PROBABILITY) {
            throw new IllegalArgumentException("probabilityBasisPoints is required");
        }
        if (rewardType == null) {
            throw new IllegalArgumentException("rewardType is required");
        }
        if (conversionMode == null) {
            throw new IllegalArgumentException("conversionMode is required");
        }
        if (conversionMode == ConversionMode.AUTO
                && (pointDelta == null || pointDelta == 0)) {
            throw new IllegalArgumentException("pointDelta is required for AUTO conversion");
        }
        if (conversionMode == ConversionMode.NONE && pointDelta != null) {
            throw new IllegalArgumentException("pointDelta must be null for NONE conversion");
        }
        if (losing && conversionMode != ConversionMode.NONE) {
            throw new IllegalArgumentException("losing option must use NONE conversion");
        }
    }

    public int safeSimulationIterations(int iterations) {
        return Math.max(MIN_SIMULATION_ITERATIONS, Math.min(iterations, MAX_SIMULATION_ITERATIONS));
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

    public RouletteProcessingStatus processingStatus(List<RouletteRoundStatus> statuses) {
        long applied = statuses.stream().filter(status -> status == RouletteRoundStatus.APPLIED).count();
        long failed = statuses.stream().filter(status -> status == RouletteRoundStatus.FAILED).count();
        if (!statuses.isEmpty() && applied == statuses.size()) {
            return RouletteProcessingStatus.APPLIED;
        }
        if (!statuses.isEmpty() && failed == statuses.size()) {
            return RouletteProcessingStatus.FAILED;
        }
        if (applied > 0 || failed > 0) {
            return RouletteProcessingStatus.PARTIALLY_APPLIED;
        }
        return RouletteProcessingStatus.CONFIRMED;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public interface ConfigCandidate {
        String triggerToken();

        Long pricePerRound();

        Integer highRoundThreshold();
    }

    public interface OptionCandidate {
        Integer probabilityBasisPoints();

        boolean losing();
    }
}
