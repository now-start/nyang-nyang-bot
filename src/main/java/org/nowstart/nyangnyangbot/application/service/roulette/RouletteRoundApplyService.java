package org.nowstart.nyangnyangbot.application.service.roulette;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AdjustFavoriteUseCase.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort.RoundResult;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.UserResult;
import org.nowstart.nyangnyangbot.application.port.out.roulette.RoulettePort;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort.CreateUserUpboCommand;
import org.nowstart.nyangnyangbot.application.port.out.upbo.UpboPort;
import org.nowstart.nyangnyangbot.domain.type.ConversionMode;
import org.nowstart.nyangnyangbot.domain.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.domain.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouletteRoundApplyService {

    private final RoulettePort roulettePort;
    private final UpboPort upboPort;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Transactional
    public void applyRound(Long roundId) {
        RoundResult round = roulettePort.findRoundById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        if (round.status() != RouletteRoundStatus.CONFIRMED) {
            return;
        }
        applyConfirmedRound(round);
    }

    private void applyConfirmedRound(RoundResult round) {
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

        UserResult savedUpbo = upboPort.createUserUpbo(new CreateUserUpboCommand(
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

    private String privateMemo(RoundResult round) {
        return "donationEventId=" + round.rouletteEventDonationEventId() + " roundNo=" + round.roundNo();
    }
}
