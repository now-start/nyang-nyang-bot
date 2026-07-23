package org.nowstart.nyangnyangbot.application.service.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.ManagePointAdjustmentPresetUseCase.ApplyPointAdjustments;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointAdjustmentPresetPort.PresetRecord;

class PointAdjustmentPresetServiceTest {

    @Test
    void applyAdjustments_AddsEveryPresetAndManualAmountExactly() {
        PointAdjustmentPresetPort presetPort = Mockito.mock(PointAdjustmentPresetPort.class);
        AdjustPointUseCase adjustPointUseCase = Mockito.mock(AdjustPointUseCase.class);
        PointAdjustmentPresetService service = new PointAdjustmentPresetService(presetPort, adjustPointUseCase);
        given(presetPort.findAll()).willReturn(List.of(
                new PresetRecord(1L, 10L, "첫째"),
                new PresetRecord(2L, 20L, "둘째")
        ));
        service.applyAdjustments(new ApplyPointAdjustments(
                "user-1", List.of(1L, 2L), 5L, "추가", "admin-1"
        ));

        ArgumentCaptor<AdjustPointCommand> captor = ArgumentCaptor.forClass(AdjustPointCommand.class);
        Mockito.verify(adjustPointUseCase).adjust(captor.capture());
        then(captor.getValue().delta()).isEqualTo(35L);
    }

    @Test
    void applyAdjustments_RejectsDuplicatePresetIdsInsteadOfTreatingThemAsMissingOrRepeated() {
        PointAdjustmentPresetPort presetPort = Mockito.mock(PointAdjustmentPresetPort.class);
        AdjustPointUseCase adjustPointUseCase = Mockito.mock(AdjustPointUseCase.class);
        PointAdjustmentPresetService service = new PointAdjustmentPresetService(presetPort, adjustPointUseCase);

        thenThrownBy(() -> service.applyAdjustments(new ApplyPointAdjustments(
                "user-1", List.of(1L, 1L, 2L, 2L), null, null, "admin-1"
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate preset ids are not allowed: [1, 2]");

        Mockito.verifyNoInteractions(presetPort, adjustPointUseCase);
    }

    @Test
    void applyAdjustments_DetectsOverflowAtEachPresetAddition() {
        PointAdjustmentPresetPort presetPort = Mockito.mock(PointAdjustmentPresetPort.class);
        AdjustPointUseCase adjustPointUseCase = Mockito.mock(AdjustPointUseCase.class);
        PointAdjustmentPresetService service = new PointAdjustmentPresetService(presetPort, adjustPointUseCase);
        given(presetPort.findAll()).willReturn(List.of(
                new PresetRecord(1L, Long.MAX_VALUE, "최대"),
                new PresetRecord(2L, 1L, "하나")
        ));

        thenThrownBy(() -> service.applyAdjustments(new ApplyPointAdjustments(
                "user-1", List.of(1L, 2L), null, null, "admin-1"
        ))).isInstanceOf(ArithmeticException.class);

        Mockito.verifyNoInteractions(adjustPointUseCase);
    }

    @Test
    void applyAdjustments_DetectsOverflowWhenAddingManualAmount() {
        PointAdjustmentPresetPort presetPort = Mockito.mock(PointAdjustmentPresetPort.class);
        AdjustPointUseCase adjustPointUseCase = Mockito.mock(AdjustPointUseCase.class);
        PointAdjustmentPresetService service = new PointAdjustmentPresetService(presetPort, adjustPointUseCase);
        given(presetPort.findAll()).willReturn(List.of(new PresetRecord(1L, Long.MAX_VALUE, "최대")));

        thenThrownBy(() -> service.applyAdjustments(new ApplyPointAdjustments(
                "user-1", List.of(1L), 1L, "하나", "admin-1"
        ))).isInstanceOf(ArithmeticException.class);

        Mockito.verifyNoInteractions(adjustPointUseCase);
    }
}
