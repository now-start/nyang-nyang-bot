package org.nowstart.nyangnyangbot.application.gateway.out.favorite;

import java.util.List;
import org.nowstart.nyangnyangbot.domain.model.FavoriteAdjustmentOption;

public interface FavoriteAdjustmentPort {

    List<FavoriteAdjustmentOption> findAll();

    FavoriteAdjustmentOption save(Integer amount, String label);

    List<FavoriteAdjustmentOption> findAllById(List<Long> ids);
}
