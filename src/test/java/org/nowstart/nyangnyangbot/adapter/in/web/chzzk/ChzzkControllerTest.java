package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase;

    @InjectMocks
    private ChzzkController chzzkController;

    @Test
    @DisplayName("치지직 채팅 수동 연결 요청 시 SUCCESS를 반환한다")
    void connect_ShouldReturnSuccess() throws URISyntaxException {
        // 실행
        ResponseEntity<String> result = chzzkController.connect();

        // 검증
        then(result.getStatusCode().is2xxSuccessful()).isTrue();
        then(result.getBody()).isEqualTo("SUCCESS");
        BDDMockito.then(connectChzzkChatSocketUseCase).should().connect();
    }

    @Test
    @DisplayName("연결 중 예외가 발생하면 예외를 전파한다")
    void connect_ShouldPropagateConnectionException() throws URISyntaxException {
        // 준비
        BDDMockito.willThrow(new URISyntaxException("bad-url", "invalid"))
                .given(connectChzzkChatSocketUseCase)
                .connect();

        // 실행 및 검증
        thenThrownBy(() -> chzzkController.connect())
                .isInstanceOf(URISyntaxException.class);
    }

    @Test
    @DisplayName("컨트롤러 수동 연결 API는 스케줄러 책임을 갖지 않는다")
    void connect_ShouldNotHaveScheduledAnnotation() throws NoSuchMethodException {
        // 실행
        boolean scheduled = ChzzkController.class
                .getMethod("connect")
                .isAnnotationPresent(Scheduled.class);

        // 검증
        then(scheduled).isFalse();
    }
}
