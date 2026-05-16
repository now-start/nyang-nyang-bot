package org.nowstart.nyangnyangbot.application.port.out.favorite;

import java.util.List;

public interface FavoriteAdjustmentPort {

    List<OptionResult> findAll();

    OptionResult save(Integer amount, String label);

    List<OptionResult> findAllById(List<Long> ids);

    record OptionResult(
            Long id,
            Integer amount,
            String label
    ) {
    }
}
