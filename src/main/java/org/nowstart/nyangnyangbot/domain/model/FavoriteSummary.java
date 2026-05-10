package org.nowstart.nyangnyangbot.domain.model;

public record FavoriteSummary(
        String userId,
        String nickName,
        Integer favorite
) {
}
