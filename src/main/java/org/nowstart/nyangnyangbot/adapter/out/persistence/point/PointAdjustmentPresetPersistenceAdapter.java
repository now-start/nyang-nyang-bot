package org.nowstart.nyangnyangbot.adapter.out.persistence.point;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.entity.PointAdjustmentPreset;
import org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointAdjustmentPresetRepository;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort;
import org.nowstart.nyangnyangbot.config.cache.CacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointAdjustmentPresetPersistenceAdapter implements PointAdjustmentPresetPort {

    private final PointAdjustmentPresetRepository presetRepository;
    private final OutboundContractValidator contractValidator;

    @Override
    @Cacheable(cacheNames = CacheNames.POINT_ADJUSTMENT_PRESETS)
    public List<PresetRecord> findAll() {
        return presetRepository.findAll().stream().map(this::record).toList();
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.POINT_ADJUSTMENT_PRESETS, allEntries = true)
    public PresetRecord save(SavePresetCommand command) {
        contractValidator.request("pointAdjustmentPreset.save", command);
        return record(presetRepository.save(PointAdjustmentPreset.builder()
                .amount(command.amount())
                .label(command.label())
                .build()));
    }

    private PresetRecord record(PointAdjustmentPreset preset) {
        return contractValidator.persistenceResult(
                "pointAdjustmentPreset.record",
                new PresetRecord(preset.getId(), preset.getAmount(), preset.getLabel())
        );
    }
}
