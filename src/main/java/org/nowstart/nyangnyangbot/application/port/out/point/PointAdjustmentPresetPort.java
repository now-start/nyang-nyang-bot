package org.nowstart.nyangnyangbot.application.port.out.point;

import java.util.List;

public interface PointAdjustmentPresetPort {

    List<PresetRecord> findAll();

    PresetRecord save(long amount, String label);

    record PresetRecord(long id, long amount, String label) {
    }
}
