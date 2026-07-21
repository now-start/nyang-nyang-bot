package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.SaveAuthorizationCommand;
import org.nowstart.nyangnyangbot.application.validation.outbound.PersistenceDataContractException;
import org.nowstart.nyangnyangbot.adapter.out.validation.OutboundContractValidator;

@ExtendWith(MockitoExtension.class)
class AuthorizationPersistenceAdapterTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Test
    void findById_ShouldMapAuthorization() {
        // 준비
        AuthorizationPersistenceAdapter adapter = adapter();
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(entity("channel-1")));

        // 실행
        AuthorizationAccountResult result = adapter.findById("channel-1").orElseThrow();

        // 검증
        then(result.channelId()).isEqualTo("channel-1");
        then(result.channelName()).isEqualTo("치즈냥");
        then(result.admin()).isTrue();
    }

    @Test
    void saveOrUpdate_ShouldCreateNewAuthorizationWhenMissing() {
        // 준비
        AuthorizationPersistenceAdapter adapter = adapter();
        AuthorizationAccount saved = entity("channel-1");
        SaveAuthorizationCommand command = command("access", "refresh", "치즈냥");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.empty());
        given(authorizationRepository.save(org.mockito.ArgumentMatchers.any(AuthorizationAccount.class))).willReturn(saved);

        // 실행
        AuthorizationAccountResult result = adapter.saveOrUpdate(command);

        // 검증
        then(result.channelId()).isEqualTo("channel-1");
        var captor = org.mockito.ArgumentCaptor.forClass(AuthorizationAccount.class);
        BDDMockito.then(authorizationRepository).should().save(captor.capture());
        then(captor.getValue().getLastLoginAt()).isNotNull();
    }

    @Test
    void saveOrUpdateAndUpdateToken_ShouldMutateExistingEntity() {
        // 준비
        AuthorizationPersistenceAdapter adapter = adapter();
        AuthorizationAccount existing = entity("channel-1");
        SaveAuthorizationCommand command = command("new-access", "new-refresh", "변경");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(existing));
        given(authorizationRepository.save(existing)).willReturn(existing);

        // 실행
        AuthorizationAccountResult saved = adapter.saveOrUpdate(command);
        AuthorizationAccountResult updated = adapter.updateToken("channel-1", command);

        // 검증
        then(saved.accessToken()).isEqualTo("new-access");
        then(updated.channelName()).isEqualTo("변경");
        then(existing.getRefreshToken()).isEqualTo("new-refresh");
        BDDMockito.then(authorizationRepository).should().save(existing);
    }

    @Test
    void saveOrUpdate_ShouldRecordLoginTimeAndTokenRefreshShouldNotChangeIt() {
        // 준비
        AuthorizationPersistenceAdapter adapter = adapter();
        AuthorizationAccount existing = entity("channel-1");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(existing));
        given(authorizationRepository.save(existing)).willReturn(existing);

        // 실행
        adapter.saveOrUpdate(command("new-access", "new-refresh", "변경"));
        LocalDateTime loggedInAt = existing.getLastLoginAt();
        adapter.updateToken("channel-1", command("refreshed-access", "refreshed-refresh", "변경"));

        // 검증
        then(loggedInAt).isNotNull();
        then(existing.getLastLoginAt()).isEqualTo(loggedInAt);
    }

    @Test
    void findById_ShouldRejectInvalidPersistedResult() {
        AuthorizationAccount invalid = AuthorizationAccount.builder()
                .channelId("channel-1")
                .channelName("치즈냥")
                .accessToken(null)
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(3600)
                .build();
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(invalid));

        thenThrownBy(() -> adapter().findById("channel-1"))
                .isInstanceOf(PersistenceDataContractException.class)
                .hasMessageContaining("accessToken is required");
    }

    private AuthorizationAccount entity(String channelId) {
        return AuthorizationAccount.builder()
                .channelId(channelId)
                .channelName("치즈냥")
                .accessToken("access")
                .refreshToken("refresh")
                .tokenType("Bearer")
                .expiresIn(3600)
                .scope("chat")
                .admin(true)
                .lastLoginAt(null)
                .build();
    }

    private AuthorizationPersistenceAdapter adapter() {
        return new AuthorizationPersistenceAdapter(
                authorizationRepository,
                new OutboundContractValidator(Validation.buildDefaultValidatorFactory().getValidator())
        );
    }

    private SaveAuthorizationCommand command(String accessToken, String refreshToken, String channelName) {
        return new SaveAuthorizationCommand(
                "channel-1", channelName, accessToken, refreshToken, "Bearer", 3600, "chat"
        );
    }
}
