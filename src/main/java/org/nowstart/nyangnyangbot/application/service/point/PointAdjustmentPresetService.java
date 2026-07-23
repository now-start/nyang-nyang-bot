package org.nowstart.nyangnyangbot.application.service.point;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort.PresetRecord;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional
public class PointAdjustmentPresetService implements ManagePointAdjustmentPresetUseCase {

    private final PointAdjustmentPresetPort presetPort;
    private final AdjustPointUseCase adjustPointUseCase;

    @Override
    @Transactional(readOnly = true)
    public List<PointAdjustmentPresetResult> getPresets() {
        return presetPort.findAll().stream()
                .sorted(java.util.Comparator.comparingLong(PresetRecord::amount))
                .map(this::result)
                .toList();
    }

    @Override
    public PointAdjustmentPresetResult createPreset(CreatePointAdjustmentPreset command) {
        return result(presetPort.save(command.amount(), command.label().trim()));
    }

    @Override
    public void applyAdjustments(ApplyPointAdjustments command) {
        List<PresetRecord> presets = selectedPresets(command.presetIds());
        long delta = 0;
        for (PresetRecord preset : presets) {
            delta = Math.addExact(delta, preset.amount());
        }
        if (command.manualAmount() != null) {
            delta = Math.addExact(delta, command.manualAmount());
        }
        String description = description(presets, command.manualAmount(), command.manualDescription());
        adjustPointUseCase.adjust(AdjustPointUseCase.AdjustPointCommand.builder()
                .userId(command.userId())
                .delta(delta)
                .sourceType(PointSourceType.ADMIN_ADJUSTMENT)
                .description(description)
                .actorUserId(command.actorUserId())
                .allowNegativeBalance(true)
                .createIfMissing(false)
                .build());
    }

    private List<PresetRecord> selectedPresets(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            LinkedHashSet<Long> seen = new LinkedHashSet<>();
            List<Long> duplicates = ids.stream().filter(id -> !seen.add(id)).distinct().toList();
            throw new IllegalArgumentException("Duplicate preset ids are not allowed: " + duplicates);
        }
        Map<Long, PresetRecord> found = presetPort.findAll().stream()
                .filter(item -> uniqueIds.contains(item.id()))
                .collect(Collectors.toMap(PresetRecord::id, item -> item));
        if (found.size() != ids.size()) {
            List<Long> missingIds = ids.stream().filter(id -> !found.containsKey(id)).toList();
            throw new IllegalArgumentException("Missing presets: " + missingIds);
        }
        return ids.stream().map(found::get).toList();
    }

    private String description(List<PresetRecord> presets, Long manualAmount, String manualDescription) {
        List<String> parts = new ArrayList<>();
        presets.forEach(item -> parts.add(String.format(Locale.ROOT, "%s(%+d)", item.label(), item.amount())));
        if (manualAmount != null && manualAmount != 0) {
            String label = manualDescription == null || manualDescription.isBlank()
                    ? "수동 입력"
                    : manualDescription.trim();
            parts.add(String.format(Locale.ROOT, "%s(%+d)", label, manualAmount));
        }
        return "포인트 조정: " + String.join(", ", parts);
    }

    private PointAdjustmentPresetResult result(PresetRecord record) {
        return new PointAdjustmentPresetResult(record.id(), record.amount(), record.label());
    }
}
