package org.nowstart.nyangnyangbot.adapter.in.scheduler.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.never;

import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class ChzzkChatConnectionSchedulerTest {

    @Mock
    private ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase;

    @Test
    @DisplayName("자동 연결이 활성화되어 있으면 치지직 채팅 연결을 시도한다")
    void scheduledConnect_ShouldConnect_WhenAutoConnectEnabled() throws URISyntaxException {
        // 준비
        ChzzkChatConnectionScheduler scheduler = new ChzzkChatConnectionScheduler(connectChzzkChatSocketUseCase, true);

        // 실행
        scheduler.scheduledConnect();

        // 검증
        BDDMockito.then(connectChzzkChatSocketUseCase).should().connect();
    }

    @Test
    @DisplayName("자동 연결이 비활성화되어 있으면 치지직 채팅 연결을 시도하지 않는다")
    void scheduledConnect_ShouldNotConnect_WhenAutoConnectDisabled() throws URISyntaxException {
        // 준비
        ChzzkChatConnectionScheduler scheduler = new ChzzkChatConnectionScheduler(connectChzzkChatSocketUseCase, false);

        // 실행
        scheduler.scheduledConnect();

        // 검증
        BDDMockito.then(connectChzzkChatSocketUseCase).should(never()).connect();
    }

    @Test
    @DisplayName("치지직 채팅 자동 연결은 1분 간격으로 실행된다")
    void scheduledConnect_ShouldRunEveryMinute() throws NoSuchMethodException {
        // 실행
        Scheduled scheduled = ChzzkChatConnectionScheduler.class
                .getMethod("scheduledConnect")
                .getAnnotation(Scheduled.class);

        // 검증
        then(scheduled).isNotNull();
        then(scheduled.fixedDelay()).isEqualTo(60_000L);
    }
}
