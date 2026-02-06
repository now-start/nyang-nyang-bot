package org.nowstart.nyangnyangbot.data.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteAdjustmentCreateRequest {

    private Integer amount;
    private String label;
}
