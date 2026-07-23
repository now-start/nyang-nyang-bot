package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;

public interface CorrectPointLedgerUseCase {

    PointLedgerResult correct(@Valid @NotNull(message = "command is required") AdjustPointCommand command);
}
