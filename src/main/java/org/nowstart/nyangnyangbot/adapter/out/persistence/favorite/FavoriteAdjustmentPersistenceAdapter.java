package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.domain.model.FavoriteAdjustmentOption;
import org.nowstart.nyangnyangbot.application.port.out.favorite.repository.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.adapter.out.persistence.entity.FavoriteAdjustmentEntity;
import org.nowstart.nyangnyangbot.adapter.out.persistence.repository.FavoriteAdjustmentRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteAdjustmentPersistenceAdapter implements FavoriteAdjustmentPort {

    private final FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Override
    public List<FavoriteAdjustmentOption> findAll() {
        return favoriteAdjustmentRepository.findAll().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    public FavoriteAdjustmentOption save(Integer amount, String label) {
        FavoriteAdjustmentEntity saved = favoriteAdjustmentRepository.save(FavoriteAdjustmentEntity.builder()
                .amount(amount)
                .label(label)
                .build());
        return toModel(saved);
    }

    @Override
    public List<FavoriteAdjustmentOption> findAllById(List<Long> ids) {
        return favoriteAdjustmentRepository.findAllById(ids).stream()
                .map(this::toModel)
                .toList();
    }

    private FavoriteAdjustmentOption toModel(FavoriteAdjustmentEntity entity) {
        return new FavoriteAdjustmentOption(entity.getId(), entity.getAmount(), entity.getLabel());
    }
}
