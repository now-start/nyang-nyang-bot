package org.nowstart.nyangnyangbot.application.port.out.favorite;

import java.util.List;
import org.nowstart.nyangnyangbot.application.model.FavoriteAdjustmentOption;

public interface FavoriteAdjustmentPort {

    List<FavoriteAdjustmentOption> findAll();

    FavoriteAdjustmentOption save(Integer amount, String label);

    List<FavoriteAdjustmentOption> findAllById(List<Long> ids);
}
