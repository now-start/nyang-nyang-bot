package org.nowstart.nyangnyangbot.adapter.in.web.chzzk;

import static org.assertj.core.api.BDDAssertions.then;

import java.net.URISyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.chzzk.ConnectChzzkChatSocketUseCase;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.scheduling.annotation.Scheduled;

@ExtendWith(MockitoExtension.class)
class ChzzkControllerTest {

    @Mock
    private ConnectChzzkChatSocketUseCase connectChzzkChatSocketUseCase;

    @InjectMocks
    private ChzzkController chzzkController;

    @Test
    @DisplayName("치지직 채팅 수동 연결 요청 시 성공 피드백 fragment를 반환한다")
    void connect_ShouldReturnSuccess() throws URISyntaxException {
        // 준비
        Model model = new ExtendedModelMap();

        // 실행
        String view = chzzkController.connect(model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.asMap().get("message")).isEqualTo("치지직 채팅 연결 완료");
        then(model.asMap().get("tone")).isEqualTo("success");
        BDDMockito.then(connectChzzkChatSocketUseCase).should().connect();
    }

    @Test
    @DisplayName("연결 중 예외가 발생하면 실패 피드백 fragment를 반환한다")
    void connect_ShouldReturnFailureFeedback_WhenConnectionFails() throws URISyntaxException {
        // 준비
        BDDMockito.willThrow(new URISyntaxException("bad-url", "invalid"))
                .given(connectChzzkChatSocketUseCase)
                .connect();
        Model model = new ExtendedModelMap();

        // 실행
        String view = chzzkController.connect(model);

        // 검증
        then(view).isEqualTo("components/feedback :: alert");
        then(model.asMap().get("message")).isEqualTo("치지직 채팅 연결 실패");
        then(model.asMap().get("tone")).isEqualTo("danger");
    }

    @Test
    @DisplayName("컨트롤러 수동 연결 API는 스케줄러 책임을 갖지 않는다")
    void connect_ShouldNotHaveScheduledAnnotation() throws NoSuchMethodException {
        // 실행
        boolean scheduled = ChzzkController.class
                .getMethod("connect", Model.class)
                .isAnnotationPresent(Scheduled.class);

        // 검증
        then(scheduled).isFalse();
    }
}
