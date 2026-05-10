package org.nowstart.nyangnyangbot.application.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nowstart.nyangnyangbot.application.model.AuthorizationAccount;
import org.nowstart.nyangnyangbot.application.model.FavoriteHistoryView;
import org.nowstart.nyangnyangbot.application.model.FavoriteSummary;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteQueryPort;
import org.nowstart.nyangnyangbot.application.dto.favorite.FavoriteHistoryDto;
import org.nowstart.nyangnyangbot.application.dto.favorite.FavoriteMeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteQueryPort favoriteQueryPort;
    private final AuthorizationPort authorizationPort;

    public Page<FavoriteSummary> getList(Pageable pageable) {
        return favoriteQueryPort.findAll(pageable);
    }

    public Page<FavoriteSummary> getByNickName(Pageable pageable, String nickName) {
        return favoriteQueryPort.findByNickNameContains(pageable, nickName);
    }

    public List<FavoriteHistoryView> getHistory(String userId, int limit) {
        return favoriteQueryPort.findHistory(userId, limit);
    }

    public FavoriteMeDto getMyFavorite(String userId) {
        AuthorizationAccount authorization = authorizationPort.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authorization user not found"));
        FavoriteSummary favorite = favoriteQueryPort.findById(userId)
                .orElse(new FavoriteSummary(userId, authorization.channelName(), 0));

        LocalDateTime lastSeenAt = authorization.favoriteHistoryLastSeenAt();
        long unseenCount = lastSeenAt == null
                ? favoriteQueryPort.countHistory(userId)
                : favoriteQueryPort.countHistoryAfter(userId, lastSeenAt);
        List<FavoriteHistoryDto> histories = getHistory(userId, 50).stream()
                .map(FavoriteHistoryDto::from)
                .toList();

        authorizationPort.markFavoriteHistorySeen(userId, LocalDateTime.now());
        return new FavoriteMeDto(
                userId,
                favorite.nickName(),
                favorite.favorite(),
                unseenCount,
                histories
        );
    }

    public java.util.Optional<String> getCurrentNickName(String userId) {
        return authorizationPort.findById(userId)
                .map(AuthorizationAccount::channelName)
                .filter(name -> !name.isBlank());
    }
}
