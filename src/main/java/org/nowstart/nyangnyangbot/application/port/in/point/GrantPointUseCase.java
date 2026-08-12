package org.nowstart.nyangnyangbot.application.port.in.point;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.PointLedgerResult;

public interface GrantPointUseCase {

    /** 일반 포인트 조정과 동일한 원장 및 멱등성 규칙으로 포인트를 지급한다. */
    PointLedgerResult grant(@Valid @NotNull(message = "command is required") AdjustPointCommand command);
}
