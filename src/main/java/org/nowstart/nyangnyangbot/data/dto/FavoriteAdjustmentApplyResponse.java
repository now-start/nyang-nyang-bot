package org.nowstart.nyangnyangbot.data.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoriteAdjustmentApplyResponse {

    private String userId;
    private Integer beforeFavorite;
    private Integer delta;
    private Integer afterFavorite;
    private String history;
}
