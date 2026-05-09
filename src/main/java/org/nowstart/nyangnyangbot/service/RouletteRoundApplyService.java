package org.nowstart.nyangnyangbot.service;

import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteCommand;
import org.nowstart.nyangnyangbot.application.favorite.AdjustFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.favorite.FavoriteLedgerResult;
import org.nowstart.nyangnyangbot.data.entity.RouletteEventEntity;
import org.nowstart.nyangnyangbot.data.entity.RouletteRoundResultEntity;
import org.nowstart.nyangnyangbot.data.entity.UserUpboEntity;
import org.nowstart.nyangnyangbot.data.type.ConversionMode;
import org.nowstart.nyangnyangbot.data.type.RouletteRoundStatus;
import org.nowstart.nyangnyangbot.data.type.UpboStatus;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.nowstart.nyangnyangbot.repository.RouletteRoundResultRepository;
import org.nowstart.nyangnyangbot.repository.UserUpboRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouletteRoundApplyService {

    private final RouletteRoundResultRepository rouletteRoundResultRepository;
    private final UserUpboRepository userUpboRepository;
    private final AdjustFavoriteUseCase adjustFavoriteUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyRound(Long roundId) {
        RouletteRoundResultEntity round = rouletteRoundResultRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("roulette round not found"));
        if (round.getStatus() != RouletteRoundStatus.CONFIRMED) {
            return;
        }
        try {
            applyConfirmedRound(round);
        } catch (RuntimeException ex) {
            round.markFailed(ex.getMessage());
        }
    }

    private void applyConfirmedRound(RouletteRoundResultEntity round) {
        if (round.isLosingItem()) {
            round.markApplied(null, null);
            return;
        }

        Long ledgerId = null;
        if (round.getConversionMode() == ConversionMode.AUTO
                && round.getExchangeFavoriteValue() != null
                && round.getExchangeFavoriteValue() != 0) {
            FavoriteLedgerResult result = adjustFavoriteUseCase.adjust(AdjustFavoriteCommand.builder()
                    .userId(round.getRouletteEvent().getUserId())
                    .nickName(round.getRouletteEvent().getNickNameSnapshot())
                    .delta(round.getExchangeFavoriteValue())
                    .sourceType(FavoriteSourceType.UPBO_ROULETTE)
                    .sourceId(String.valueOf(round.getId()))
                    .displayCategory("ROULETTE")
                    .publicDescription("룰렛 결과: " + round.getItemLabel())
                    .privateMemo("donationEventId=" + round.getRouletteEvent().getDonationEventId()
                            + " roundNo=" + round.getRoundNo())
                    .actorId("SYSTEM")
                    .idempotencyKey("roulette-round:" + round.getId())
                    .allowNegativeBalance(true)
                    .createIfMissing(true)
                    .build());
            ledgerId = result.ledgerId();
        }

        UserUpboEntity savedUpbo = userUpboRepository.save(UserUpboEntity.builder()
                .userId(round.getRouletteEvent().getUserId())
                .nickNameSnapshot(round.getRouletteEvent().getNickNameSnapshot())
                .label(round.getItemLabel())
                .status(ledgerId == null ? UpboStatus.OWNED : UpboStatus.CONVERTED)
                .exchangeFavoriteValue(round.getExchangeFavoriteValue())
                .rewardType(round.getRewardType())
                .conversionMode(round.getConversionMode())
                .sourceType(FavoriteSourceType.UPBO_ROULETTE)
                .ledgerId(ledgerId)
                .publicDescription("룰렛 결과: " + round.getItemLabel())
                .privateMemo(privateMemo(round.getRouletteEvent(), round))
                .actorId("SYSTEM")
                .build());
        round.markApplied(ledgerId, savedUpbo.getId());
    }

    private String privateMemo(RouletteEventEntity event, RouletteRoundResultEntity round) {
        return "donationEventId=" + event.getDonationEventId() + " roundNo=" + round.getRoundNo();
    }
}
