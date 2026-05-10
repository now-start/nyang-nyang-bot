package org.nowstart.nyangnyangbot.application.favorite;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.CheckIdempotencyPort;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.LoadFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.SaveFavoriteAccountPort;
import org.nowstart.nyangnyangbot.application.gateway.out.favorite.SaveFavoriteLedgerPort;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteAccount;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteBalanceChange;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteLedgerEntry;
import org.nowstart.nyangnyangbot.domain.favorite.FavoriteSourceType;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteLedgerService implements AdjustFavoriteUseCase, GrantFavoriteUseCase, CorrectFavoriteLedgerUseCase {

    private final LoadFavoriteAccountPort loadFavoriteAccountPort;
    private final SaveFavoriteAccountPort saveFavoriteAccountPort;
    private final SaveFavoriteLedgerPort saveFavoriteLedgerPort;
    private final CheckIdempotencyPort checkIdempotencyPort;

    @Override
    public FavoriteLedgerResult adjust(AdjustFavoriteCommand command) {
        validate(command);
        if (hasText(command.idempotencyKey()) && checkIdempotencyPort.existsByIdempotencyKey(command.idempotencyKey())) {
            return FavoriteLedgerResult.duplicate(command.userId());
        }

        FavoriteAccount account = loadFavoriteAccountPort.loadForUpdate(command.userId())
                .orElseGet(() -> {
                    if (!command.createIfMissing()) {
                        throw new IllegalArgumentException("Favorite user not found");
                    }
                    return FavoriteAccount.of(command.userId(), command.nickName(), 0);
                });
        account.updateNickName(command.nickName());

        FavoriteBalanceChange change = account.applyDelta(command.delta(), command.allowNegativeBalance());
        saveFavoriteAccountPort.save(account);

        String history = resolveHistory(command);
        FavoriteLedgerEntry savedLedger = saveFavoriteLedgerPort.save(FavoriteLedgerEntry.builder()
                .userId(account.getUserId())
                .delta(change.delta())
                .balanceAfter(change.afterBalance())
                .sourceType(command.sourceType())
                .sourceId(command.sourceId())
                .displayCategory(command.displayCategory())
                .publicDescription(history)
                .privateMemo(command.privateMemo())
                .correctionOfLedgerId(command.correctionOfLedgerId())
                .actorId(command.actorId())
                .idempotencyKey(command.idempotencyKey())
                .nickNameSnapshot(account.getNickName())
                .build());

        return new FavoriteLedgerResult(
                account.getUserId(),
                change.beforeBalance(),
                change.delta(),
                change.afterBalance(),
                history,
                false,
                savedLedger.id()
        );
    }

    @Override
    public FavoriteLedgerResult grant(AdjustFavoriteCommand command) {
        return adjust(command);
    }

    @Override
    public FavoriteLedgerResult correct(AdjustFavoriteCommand command) {
        AdjustFavoriteCommand correctionCommand = AdjustFavoriteCommand.builder()
                .userId(command.userId())
                .nickName(command.nickName())
                .delta(command.delta())
                .sourceType(FavoriteSourceType.CORRECTION)
                .sourceId(command.sourceId())
                .displayCategory(command.displayCategory())
                .publicDescription(command.publicDescription())
                .privateMemo(command.privateMemo())
                .correctionOfLedgerId(command.correctionOfLedgerId())
                .actorId(command.actorId())
                .idempotencyKey(command.idempotencyKey())
                .allowNegativeBalance(true)
                .createIfMissing(false)
                .build();
        return adjust(correctionCommand);
    }

    private void validate(AdjustFavoriteCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        if (!hasText(command.userId())) {
            throw new IllegalArgumentException("userId is required");
        }
        if (command.delta() == 0) {
            throw new IllegalArgumentException("delta must not be zero");
        }
        if (command.sourceType() == null) {
            throw new IllegalArgumentException("sourceType is required");
        }
    }

    private String resolveHistory(AdjustFavoriteCommand command) {
        if (hasText(command.publicDescription())) {
            return command.publicDescription();
        }
        return switch (command.sourceType()) {
            case ATTENDANCE -> "출석체크";
            case SHEET_MIGRATION -> "데이터 동기화";
            case ADMIN_ADJUSTMENT -> "관리자 조정";
            case UPBO_MANUAL -> "업보 적용";
            case UPBO_ROULETTE -> "룰렛 결과";
            case CORRECTION -> "호감도 정정";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
