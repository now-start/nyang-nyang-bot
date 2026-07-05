package org.nowstart.nyangnyangbot.adapter.out.persistence.favorite;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.entity.FavoriteAdjustment;
import org.nowstart.nyangnyangbot.adapter.out.persistence.favorite.repository.FavoriteAdjustmentRepository;
import org.nowstart.nyangnyangbot.application.port.out.favorite.FavoriteAdjustmentPort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteAdjustmentPersistenceAdapter implements FavoriteAdjustmentPort {

    private final FavoriteAdjustmentRepository favoriteAdjustmentRepository;

    @Override
    @Cacheable(cacheNames = CacheNames.FAVORITE_ADJUSTMENTS)
    public List<OptionResult> findAll() {
        return favoriteAdjustmentRepository.findAll().stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.FAVORITE_ADJUSTMENTS, allEntries = true)
    public OptionResult save(Integer amount, String label) {
        FavoriteAdjustment saved = favoriteAdjustmentRepository.save(FavoriteAdjustment.builder()
                .amount(amount)
                .label(label)
                .build());
        return toModel(saved);
    }

    private OptionResult toModel(FavoriteAdjustment entity) {
        return new OptionResult(entity.getId(), entity.getAmount(), entity.getLabel());
    }
}
