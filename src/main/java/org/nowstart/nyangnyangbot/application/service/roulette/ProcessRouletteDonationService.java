package org.nowstart.nyangnyangbot.application.service.roulette;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.overlay.QueueOverlayDisplayUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase;
import org.nowstart.nyangnyangbot.application.port.in.roulette.ProcessRouletteDonationUseCase.RouletteRunResult;
import org.nowstart.nyangnyangbot.application.port.in.roulette.QueryRouletteResultUseCase.RouletteRoundResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.DonationEventPayload;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteEventCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.CreateRouletteRoundCommand;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.EventResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.ItemResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.TableResult;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteActivationValidation;
import org.nowstart.nyangnyangbot.domain.roulette.RouletteItemSnapshot;
import org.nowstart.nyangnyangbot.domain.roulette.RoulettePolicy;
import org.nowstart.nyangnyangbot.domain.type.RouletteEventStatus;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessRouletteDonationService implements ProcessRouletteDonationUseCase {

    private final RoulettePolicy roulettePolicy = new RoulettePolicy();
    private final ObjectMapper objectMapper;
    private final RoulettePort roulettePort;
    private final RouletteRoundApplyService rouletteRoundApplyService;
    private final RouletteEventStatusService rouletteEventStatusService;
    private final QueueOverlayDisplayUseCase queueOverlayDisplayUseCase;

    @Override
    @Transactional
    public RouletteRunResult processDonation(DonationEventPayload donation) {
        if (donation == null) {
            return RouletteRunResult.ignored("donation is required");
        }
        if (isBlank(donation.donationEventId())) {
            return RouletteRunResult.ignored("donation event id is required");
        }
        TableResult table = roulettePort.findLatestActiveTable()
                .orElse(null);
        if (table == null) {
            return RouletteRunResult.ignored("active roulette table not found");
        }
        if (!roulettePolicy.containsCommand(donation.donationText(), table.command())) {
            return RouletteRunResult.ignored("roulette command not found");
        }
        if (roulettePort.existsEventByDonationEventId(donation.donationEventId())) {
            return RouletteRunResult.duplicate();
        }

        long amount = roulettePolicy.parseDonationAmount(donation.payAmount());
        int roundCount = roulettePolicy.calculateRoundCount(amount, table.pricePerRound());
        if (roundCount < 1) {
            return RouletteRunResult.ignored("donation amount is less than roulette price");
        }
        if (roundCount > roulettePolicy.highRoundThreshold(table)) {
            log.info("action=roulette.high_round donationEventId={} roundCount={}",
                    donation.donationEventId(), roundCount);
        }

        List<ItemResult> items = roulettePort.findActiveItemsByTableId(table.id());
        RouletteActivationValidation validation = roulettePolicy.validateActivation(table, items);
        if (!validation.activatable()) {
            return RouletteRunResult.ignored("active roulette table is invalid");
        }

        EventResult event = roulettePort.createEvent(new CreateRouletteEventCommand(
                donation.donationEventId(),
                donation.donationEventId(),
                donation.donatorChannelId(),
                trimToEmpty(donation.donatorNickname()),
                amount,
                donation.donationText(),
                table.id(),
                table.version(),
                table.command(),
                table.pricePerRound(),
                roundCount,
                toJson(items.stream().map(this::itemSnapshot).toList()),
                RouletteEventStatus.CONFIRMED
        ));
        List<RoundResult> rounds = roulettePort.saveRounds(event.id(), confirmRounds(event, items));
        rounds.forEach(round -> rouletteRoundApplyService.applyRound(round.id()));
        rouletteEventStatusService.refreshEventStatus(event.id());
        queueOverlayDisplayUseCase.enqueueRouletteEvent(event.id());
        log.info("action=roulette.run result=confirmed donationEventId={} rouletteEventId={} roundCount={}",
                donation.donationEventId(), event.id(), roundCount);
        return RouletteRunResult.confirmed(
                event.id(),
                event.roundCount(),
                rounds.stream().map(this::roundResult).toList()
        );
    }

    private List<CreateRouletteRoundCommand> confirmRounds(EventResult event, List<ItemResult> items) {
        List<CreateRouletteRoundCommand> rounds = new ArrayList<>();
        for (int roundNo = 1; roundNo <= event.roundCount(); roundNo++) {
            int ticket = roulettePolicy.nextTicket(RoulettePolicy.TOTAL_PROBABILITY);
            ItemResult selected = roulettePolicy.selectItem(items, ticket);
            rounds.add(new CreateRouletteRoundCommand(
                    roundNo,
                    selected.label(),
                    selected.probabilityBasisPoints(),
                    selected.losingItem(),
                    selected.rewardType(),
                    selected.conversionMode(),
                    selected.exchangeFavoriteValue(),
                    RouletteRoundStatus.CONFIRMED,
                    ticket
            ));
        }
        return rounds;
    }

    private RouletteItemSnapshot itemSnapshot(ItemResult item) {
        return new RouletteItemSnapshot(
                item.id(),
                item.label(),
                item.probabilityBasisPoints(),
                item.losingItem(),
                item.rewardType(),
                item.conversionMode(),
                item.exchangeFavoriteValue(),
                item.active(),
                item.displayOrder()
        );
    }

    private RouletteRoundResult roundResult(RoundResult round) {
        return new RouletteRoundResult(
                round.id(),
                round.roundNo(),
                round.itemLabel(),
                round.losingItem(),
                round.rewardType() == null ? null : round.rewardType().name(),
                round.conversionMode() == null ? null : round.conversionMode().name(),
                round.exchangeFavoriteValue(),
                round.status() == null ? null : round.status().name(),
                round.ledgerId(),
                round.userUpboId(),
                round.failureReason()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize roulette snapshot", ex);
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
