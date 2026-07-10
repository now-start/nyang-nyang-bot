package org.nowstart.nyangnyangbot.application.service.favorite;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.port.in.favorite.AcknowledgeFavoriteHistoryUseCase;
import org.nowstart.nyangnyangbot.application.port.in.favorite.QueryFavoriteUseCase;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.HistoryResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort.SummaryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService implements QueryFavoriteUseCase, AcknowledgeFavoriteHistoryUseCase {

    private final FavoriteQueryPort favoriteQueryPort;
    private final AuthorizationPort authorizationPort;

    @Override
    public Page<FavoriteSummaryResult> getList(Pageable pageable) {
        return favoriteQueryPort.findAll(pageable).map(this::favoriteSummaryResult);
    }

    @Override
    public Page<FavoriteSummaryResult> getByNickName(Pageable pageable, String nickName) {
        return favoriteQueryPort.findByNickNameContains(pageable, nickName).map(this::favoriteSummaryResult);
    }

    @Override
    public List<FavoriteHistoryResult> getHistory(String userId, int limit) {
        return favoriteQueryPort.findHistory(userId, limit).stream()
                .map(this::favoriteHistoryResult)
                .toList();
    }

    @Override
    public FavoriteMeResult getMyFavorite(String userId) {
        AuthorizationAccountResult authorization = authorizationPort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authorization user not found"));
        SummaryResult favorite = favoriteQueryPort.findById(userId)
                .orElse(new SummaryResult(userId, authorization.channelName(), 0));

        LocalDateTime lastSeenAt = authorization.favoriteHistoryLastSeenAt();
        long unseenCount = lastSeenAt == null
                ? favoriteQueryPort.countHistory(userId)
                : favoriteQueryPort.countHistoryAfter(userId, lastSeenAt);
        int favoriteValue = favorite.favorite() == null ? 0 : favorite.favorite();
        int rank = Math.toIntExact(favoriteQueryPort.countByFavoriteGreaterThan(favoriteValue) + 1);
        List<FavoriteHistoryResult> histories = getHistory(userId, 50);

        return new FavoriteMeResult(
                userId,
                favorite.nickName(),
                favorite.favorite(),
                rank,
                unseenCount,
                histories
        );
    }

    @Override
    public void acknowledgeHistory(String userId) {
        authorizationPort.markFavoriteHistorySeen(userId, LocalDateTime.now());
    }

    @Override
    public java.util.Optional<String> getCurrentNickName(String userId) {
        return authorizationPort.findById(userId)
                .map(AuthorizationAccountResult::channelName)
                .filter(name -> !name.isBlank());
    }

    private FavoriteSummaryResult favoriteSummaryResult(SummaryResult result) {
        return new FavoriteSummaryResult(result.userId(), result.nickName(), result.favorite());
    }

    private FavoriteHistoryResult favoriteHistoryResult(HistoryResult view) {
        return new FavoriteHistoryResult(
                view.ledgerId(),
                view.channelId(),
                view.nickNameSnapshot(),
                view.delta(),
                view.balanceAfter(),
                view.sourceType() == null ? null : view.sourceType().name(),
                view.displayCategory(),
                view.publicDescription(),
                view.correction(),
                view.favorite(),
                view.history(),
                view.createdAt()
        );
    }
}
