package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort.OptionResult;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteAdjustmentPersistenceAdapter implements FavoriteAdjustmentPort {

    private final FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Override
    public List<OptionResult> findAll() {
        return favoriteAdjustmentRepository.findAll().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public OptionResult save(Integer amount, String label) {
        FavoriteAdjustment saved = favoriteAdjustmentRepository.save(FavoriteAdjustment.builder()
                .amount(amount)
                .label(label)
                .build());
        return toModel(saved);
    }

    @Override
    public List<OptionResult> findAllById(List<Long> ids) {
        return favoriteAdjustmentRepository.findAllById(ids).stream()
                .map(this::toModel)
                .toList();
    }

    private OptionResult toModel(FavoriteAdjustment entity) {
        return new OptionResult(entity.getId(), entity.getAmount(), entity.getLabel());
    }
}
