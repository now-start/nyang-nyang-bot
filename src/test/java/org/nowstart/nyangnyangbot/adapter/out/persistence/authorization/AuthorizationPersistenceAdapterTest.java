package org.nowstart.nyangnyangbot.adapter.out.persistence.authorization;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.entity.AuthorizationAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.authorization.repository.AuthorizationRepository;
import org.nowstart.nyangnyangbot.application.port.out.authorization.AuthorizationPort.AuthorizationAccountResult;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.AuthorizationToken;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort.UserResult;

@ExtendWith(MockitoExtension.class)
class AuthorizationPersistenceAdapterTest {

    @Mock
    private AuthorizationRepository authorizationRepository;

    @Test
    void findById_ShouldMapAuthorization() {
        // 준비
        AuthorizationPersistenceAdapter adapter = new AuthorizationPersistenceAdapter(authorizationRepository);
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
        AuthorizationPersistenceAdapter adapter = new AuthorizationPersistenceAdapter(authorizationRepository);
        AuthorizationAccount saved = entity("channel-1");
        AuthorizationToken token = token("access", "refresh");
        UserResult user = new UserResult("channel-1", "치즈냥", "ACTIVE");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.empty());
        given(authorizationRepository.save(org.mockito.ArgumentMatchers.any(AuthorizationAccount.class))).willReturn(saved);

        // 실행
        AuthorizationAccountResult result = adapter.saveOrUpdate(user, token);

        // 검증
        then(result.channelId()).isEqualTo("channel-1");
        BDDMockito.then(authorizationRepository).should().save(org.mockito.ArgumentMatchers.any(AuthorizationAccount.class));
    }

    @Test
    void saveOrUpdateAndUpdateToken_ShouldMutateExistingEntity() {
        // 준비
        AuthorizationPersistenceAdapter adapter = new AuthorizationPersistenceAdapter(authorizationRepository);
        AuthorizationAccount existing = entity("channel-1");
        AuthorizationToken updatedToken = token("new-access", "new-refresh");
        UserResult user = new UserResult("channel-1", "변경", "ACTIVE");
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(existing));

        // 실행
        AuthorizationAccountResult saved = adapter.saveOrUpdate(user, updatedToken);
        AuthorizationAccountResult updated = adapter.updateToken("channel-1", user, updatedToken);

        // 검증
        then(saved.accessToken()).isEqualTo("new-access");
        then(updated.channelName()).isEqualTo("변경");
        then(existing.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void markFavoriteHistorySeen_ShouldUpdateOnlyWhenEntityExists() {
        // 준비
        AuthorizationPersistenceAdapter adapter = new AuthorizationPersistenceAdapter(authorizationRepository);
        AuthorizationAccount existing = entity("channel-1");
        LocalDateTime seenAt = LocalDateTime.of(2026, 5, 16, 23, 40);
        given(authorizationRepository.findById("channel-1")).willReturn(Optional.of(existing));
        given(authorizationRepository.findById("missing")).willReturn(Optional.empty());

        // 실행
        adapter.markFavoriteHistorySeen("channel-1", seenAt);
        adapter.markFavoriteHistorySeen("missing", seenAt);

        // 검증
        then(existing.getFavoriteHistoryLastSeenAt()).isEqualTo(seenAt);
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
                .favoriteHistoryLastSeenAt(null)
                .build();
    }

    private AuthorizationToken token(String accessToken, String refreshToken) {
        return new AuthorizationToken(accessToken, refreshToken, "Bearer", 3600, "chat");
    }
}
