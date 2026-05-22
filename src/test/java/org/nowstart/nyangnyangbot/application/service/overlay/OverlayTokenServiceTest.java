package org.nowstart.nyangnyangbot.application.service.overlay;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.application.port.in.overlay.IssueOverlayTokenUseCase.OverlayTokenIssueResult;
import org.nowstart.nyangnyangbot.application.port.out.overlay.OverlayTokenPort;

@ExtendWith(MockitoExtension.class)
class OverlayTokenServiceTest {

    @Mock
    private OverlayTokenPort overlayTokenPort;

    @Test
    void issueToken_원문_토큰이_아닌_해시만_저장하고_기존_토큰을_무효화한다() {
        // given: 저장 시 tokenId=2 반환
        given(overlayTokenPort.saveIssuedToken(any(), any())).willReturn(2L);
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);

        // when: 어드민 토큰 발급
        OverlayTokenIssueResult result = service.issueToken("admin-1");

        // then: 원문 토큰 반환, 저장된 값은 해시(원문과 다름), 기존 토큰 무효화 호출
        then(result.token()).isNotBlank();
        then(result.tokenId()).isEqualTo(2L);
        BDDMockito.then(overlayTokenPort).should().revokeActive(any());
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        BDDMockito.then(overlayTokenPort).should().saveIssuedToken(hashCaptor.capture(), argThat("admin-1"::equals));
        then(hashCaptor.getValue()).isNotEqualTo(result.token());
    }

    @Test
    void validateToken_원문_토큰의_해시가_없으면_false를_반환한다() {
        // given: 해시가 활성 상태로 없음
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);
        given(overlayTokenPort.existsActiveTokenHash(any())).willReturn(false);

        // when: 원문 토큰으로 검증
        boolean valid = service.validateToken("unknown-token");

        // then: 유효하지 않음
        then(valid).isFalse();
    }

    @Test
    void validateToken_원문_토큰의_해시가_활성_토큰으로_존재하면_true를_반환한다() {
        // given: 특정 해시가 활성 상태로 존재
        OverlayTokenService service = new OverlayTokenService(overlayTokenPort);
        String hash = service.hashToken("raw-token");
        given(overlayTokenPort.existsActiveTokenHash(hash)).willReturn(true);

        // when: 원문 토큰으로 검증
        boolean valid = service.validateToken("raw-token");

        // then: 유효, 해시값으로 조회 확인
        then(valid).isTrue();
        BDDMockito.then(overlayTokenPort).should().existsActiveTokenHash(argThat(hash::equals));
    }
}
