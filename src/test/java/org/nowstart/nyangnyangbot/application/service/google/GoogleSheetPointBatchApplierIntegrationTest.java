package org.nowstart.nyangnyangbot.application.service.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase;
import org.nowstart.nyangnyangbot.application.port.in.point.AdjustPointUseCase.AdjustPointCommand;
import org.nowstart.nyangnyangbot.application.port.in.point.QueryPointUseCase;
import org.nowstart.nyangnyangbot.application.port.out.google.GoogleSheetPort.GoogleSheetRow;
import org.nowstart.nyangnyangbot.domain.point.PointSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class GoogleSheetPointBatchApplierIntegrationTest {

    @Autowired
    private GoogleSheetPointBatchApplier batchApplier;

    @Autowired
    private AdjustPointUseCase adjustPointUseCase;

    @Autowired
    private QueryPointUseCase queryPointUseCase;

    @Test
    void applyRollsBackEarlierRowsWhenLaterReconciliationFails() {
        String firstUserId = "sheet-atomic-first";
        String overflowUserId = "sheet-atomic-overflow";
        adjustPointUseCase.adjust(AdjustPointCommand.builder()
                .userId(overflowUserId)
                .displayName("overflow")
                .delta(Long.MIN_VALUE)
                .sourceType(PointSourceType.ADMIN_ADJUSTMENT)
                .description("atomic batch fixture")
                .idempotencyKey("sheet-atomic-fixture")
                .allowNegativeBalance(true)
                .createIfMissing(true)
                .build());

        assertThatThrownBy(() -> batchApplier.apply(List.of(
                new GoogleSheetRow("first", firstUserId, 10L),
                new GoogleSheetRow("overflow", overflowUserId, Long.MAX_VALUE)
        ))).isInstanceOf(ArithmeticException.class);

        assertThat(queryPointUseCase.getCurrentDisplayName(firstUserId)).isEmpty();
        assertThat(queryPointUseCase.getMyPoint(overflowUserId).point()).isEqualTo(Long.MIN_VALUE);
    }
}
