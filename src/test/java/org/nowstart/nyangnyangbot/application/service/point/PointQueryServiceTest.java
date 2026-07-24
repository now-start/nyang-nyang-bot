package org.nowstart.nyangnyangbot.application.service.point;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort;
import org.nowstart.nyangnyangbot.application.port.out.point.PointQueryPort.PointSummaryRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.UserAccountPort;

@ExtendWith(MockitoExtension.class)
class PointQueryServiceTest {

    @Mock
    private PointQueryPort pointQueryPort;

    @Mock
    private UserAccountPort userAccountPort;

    private PointQueryService service;

    @BeforeEach
    void setUp() {
        service = new PointQueryService(pointQueryPort, userAccountPort);
    }

    @Test
    void getMyPoint_ReturnsSummaryWithoutLoadingHistory() {
        given(pointQueryPort.findByUserId("user-1"))
                .willReturn(Optional.of(new PointSummaryRecord("user-1", "냥이", 100L)));
        given(pointQueryPort.countByBalanceGreaterThan(100L)).willReturn(6L);

        var result = service.getMyPoint("user-1");

        then(result.userId()).isEqualTo("user-1");
        then(result.displayName()).isEqualTo("냥이");
        then(result.point()).isEqualTo(100L);
        then(result.rank()).isEqualTo(7L);
        org.mockito.BDDMockito.then(pointQueryPort).should(never()).findHistory("user-1", 50);
    }
}
