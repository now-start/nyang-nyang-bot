package org.nowstart.nyangnyangbot.data.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteAdjustmentApplyRequest {

    private String userId;
    private List<Long> adjustmentIds;
    private Integer manualAmount;
    private String manualHistory;
}
