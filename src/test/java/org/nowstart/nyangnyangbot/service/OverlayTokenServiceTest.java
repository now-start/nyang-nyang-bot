package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.data.dto.overlay.OverlayTokenDto;
import org.nowstart.nyangnyangbot.data.entity.OverlayTokenEntity;
import org.nowstart.nyangnyangbot.repository.OverlayTokenRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OverlayTokenServiceTest {

    @Mock
    private OverlayTokenRepository overlayTokenRepository;

    @Test
    void issueToken_ShouldStoreOnlyTokenHashAndRevokeActiveTokens() {
        OverlayTokenEntity oldToken = OverlayTokenEntity.builder()
                .id(1L)
                .tokenHash("old")
                .active(true)
                .build();
        given(overlayTokenRepository.findByActiveTrue()).willReturn(List.of(oldToken));
        given(overlayTokenRepository.save(any(OverlayTokenEntity.class))).willAnswer(invocation -> {
            OverlayTokenEntity entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 2L);
            return entity;
        });
        OverlayTokenService service = new OverlayTokenService(overlayTokenRepository);

        OverlayTokenDto.IssueResponse response = service.issueToken("admin-1");

        assertThat(response.token()).isNotBlank();
        assertThat(oldToken.isActive()).isFalse();
        ArgumentCaptor<OverlayTokenEntity> captor = ArgumentCaptor.forClass(OverlayTokenEntity.class);
        then(overlayTokenRepository).should().save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(response.token());
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(captor.getValue().getIssuedBy()).isEqualTo("admin-1");
    }

    @Test
    void validateToken_ShouldCheckHashAgainstActiveToken() {
        OverlayTokenService service = new OverlayTokenService(overlayTokenRepository);
        String hash = service.hashToken("raw-token");
        given(overlayTokenRepository.existsByTokenHashAndActiveTrue(hash)).willReturn(true);

        assertThat(service.validateToken("raw-token")).isTrue();

        then(overlayTokenRepository).should().existsByTokenHashAndActiveTrue(argThat(hash::equals));
    }
}
