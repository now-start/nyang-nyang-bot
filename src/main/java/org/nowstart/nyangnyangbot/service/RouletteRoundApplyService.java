package org.nowstart.nyangnyangbot.service;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.model.RouletteRound;
import org.nowstart.nyangnyangbot.application.model.UserUpbo;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouletteRoundApplyService {

    private final RoulettePort roulettePort;
    private final UpboPort upboPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyRound(Long roundId) {
        RouletteRound round = roulettePort.findRoundById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        if (round.status() != RouletteRoundStatus.CONFIRMED) {
            return;
        }
        try {
            applyConfirmedRound(round);
        } catch (RuntimeException ex) {
            roulettePort.markRoundFailed(round.id(), ex.getMessage());
        }
    }

    private void applyConfirmedRound(RouletteRound round) {
        if (round.losingItem()) {
            roulettePort.markRoundApplied(round.id(), null, null);
            return;
        }

        Long ledgerId = null;
        if (round.conversionMode() == ConversionMode.AUTO
                && round.exchangeFavoriteValue() != null
                && round.exchangeFavoriteValue() != 0) {
            FavoriteLedgerResult result = adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                    .userId(round.rouletteEventUserId())
                    .nickName(round.rouletteEventNickNameSnapshot())
                    .delta(round.exchangeFavoriteValue())
                    .sourceType(FavoriteSourceType.UPBO_ROULETTE)
                    .sourceId(String.valueOf(round.id()))
                    .displayCategory("ROULETTE")
                    .publicDescription("룰렛 결과: " + round.itemLabel())
                    .privateMemo(privateMemo(round))
                    .actorId("SYSTEM")
                    .idempotencyKey("roulette-round:" + round.id())
                    .allowNegativeBalance(true)
                    .createIfMissing(true)
                    .build());
            ledgerId = result.ledgerId();
        }

        UserUpbo savedUpbo = upboPort.createUserUpbo(new CreateUserUpboCommand(
                round.rouletteEventUserId(),
                null,
                round.rouletteEventNickNameSnapshot(),
                round.itemLabel(),
                ledgerId == null ? UpboStatus.OWNED : UpboStatus.CONVERTED,
                round.exchangeFavoriteValue(),
                round.rewardType(),
                round.conversionMode(),
                FavoriteSourceType.UPBO_ROULETTE,
                ledgerId,
                "룰렛 결과: " + round.itemLabel(),
                privateMemo(round),
                "SYSTEM"
        ));
        roulettePort.markRoundApplied(round.id(), ledgerId, savedUpbo.id());
    }

    private String privateMemo(RouletteRound round) {
        return "donationEventId=" + round.rouletteEventDonationEventId() + " roundNo=" + round.roundNo();
    }
}
