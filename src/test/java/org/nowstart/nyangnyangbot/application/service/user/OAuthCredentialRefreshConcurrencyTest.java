package org.nowstart.nyangnyangbot.application.service.user;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.OAuthCredential;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.OAuthCredentialRepository;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.nowstart.nyangnyangbot.application.port.out.chzzk.ChzzkClientPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.OAuthCredentialRecord;
import org.nowstart.nyangnyangbot.application.port.out.user.OAuthCredentialPort.SaveOAuthCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "nyang.local-dummy-data.enabled=false")
@ActiveProfiles("local")
@Import(OAuthCredentialRefreshConcurrencyTest.FakeProviderConfiguration.class)
class OAuthCredentialRefreshConcurrencyTest {

    private static final String USER_ID = "oauth-concurrency-user";

    @Autowired
    private OAuthCredentialRefreshCoordinator coordinator;
    @Autowired
    private OAuthCredentialPort credentialPort;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OAuthCredentialRepository credentialRepository;
    @Autowired
    private BlockingOneTimeChzzkClient chzzkClient;

    @BeforeEach
    void setUp() {
        credentialRepository.deleteById(USER_ID);
        userAccountRepository.deleteById(USER_ID);
        UserAccount user = userAccountRepository.saveAndFlush(UserAccount.builder()
                .userId(USER_ID)
                .displayName("냥이")
                .admin(false)
                .build());
        credentialRepository.saveAndFlush(OAuthCredential.builder()
                .userAccount(user)
                .accessToken("expired-access")
                .refreshToken("one-time-refresh")
                .tokenType("Bearer")
                .scope("scope")
                .accessTokenExpiresAt(Instant.now().minusSeconds(60))
                .build());
        chzzkClient.reset();
    }

    @AfterEach
    void tearDown() {
        chzzkClient.releaseProvider();
        credentialRepository.deleteById(USER_ID);
        userAccountRepository.deleteById(USER_ID);
    }

    @Test
    void concurrentRefreshes_ConsumeOneTimeRefreshTokenExactlyOnce() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch bothInvoked = new CountDownLatch(2);
        Future<OAuthCredentialRecord> first = executor.submit(() -> refreshAfter(start, bothInvoked));
        Future<OAuthCredentialRecord> second = executor.submit(() -> refreshAfter(start, bothInvoked));

        try {
            start.countDown();
            then(bothInvoked.await(5, TimeUnit.SECONDS)).isTrue();
            then(chzzkClient.awaitProviderCall()).isTrue();
            then(chzzkClient.awaitRejectedSecondUse(500, TimeUnit.MILLISECONDS)).isFalse();
            chzzkClient.releaseProvider();

            OAuthCredentialRecord firstResult = first.get(5, TimeUnit.SECONDS);
            OAuthCredentialRecord secondResult = second.get(5, TimeUnit.SECONDS);
            then(firstResult.accessToken()).isEqualTo("fresh-access");
            then(secondResult.accessToken()).isEqualTo("fresh-access");
            then(firstResult.credentialVersion()).isEqualTo(1);
            then(secondResult.credentialVersion()).isEqualTo(1);
            then(chzzkClient.refreshAttempts()).isEqualTo(1);
            OAuthCredential saved = credentialRepository.findById(USER_ID).orElseThrow();
            then(saved.getAccessToken()).isEqualTo("fresh-access");
            then(saved.getRefreshToken()).isEqualTo("next-refresh");
            then(saved.getCredentialVersion()).isEqualTo(1);
        } finally {
            chzzkClient.releaseProvider();
            executor.shutdownNow();
        }
    }

    @Test
    void updateToken_WhenObservedVersionIsStaleRejectsUpdateAndPreservesCredential() {
        SaveOAuthCredential attemptedUpdate = new SaveOAuthCredential(
                USER_ID,
                "냥이",
                "stale-access",
                "stale-refresh",
                "Bearer",
                3600,
                "scope"
        );

        thenThrownBy(() -> credentialPort.updateToken(USER_ID, 99, attemptedUpdate))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        OAuthCredential saved = credentialRepository.findById(USER_ID).orElseThrow();
        then(saved.getAccessToken()).isEqualTo("expired-access");
        then(saved.getRefreshToken()).isEqualTo("one-time-refresh");
    }

    private OAuthCredentialRecord refreshAfter(CountDownLatch start, CountDownLatch bothInvoked) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        bothInvoked.countDown();
        return coordinator.refreshIfExpired(USER_ID);
    }

    @TestConfiguration
    static class FakeProviderConfiguration {

        @Bean
        @Primary
        BlockingOneTimeChzzkClient blockingOneTimeChzzkClient() {
            return new BlockingOneTimeChzzkClient();
        }
    }

    static class BlockingOneTimeChzzkClient implements ChzzkClientPort {

        private final AtomicInteger refreshAttempts = new AtomicInteger();
        private CountDownLatch providerCalled;
        private CountDownLatch providerRelease;
        private CountDownLatch rejectedSecondUse;

        void reset() {
            refreshAttempts.set(0);
            providerCalled = new CountDownLatch(1);
            providerRelease = new CountDownLatch(1);
            rejectedSecondUse = new CountDownLatch(1);
        }

        boolean awaitProviderCall() throws InterruptedException {
            return providerCalled.await(5, TimeUnit.SECONDS);
        }

        boolean awaitRejectedSecondUse(long timeout, TimeUnit unit) throws InterruptedException {
            return rejectedSecondUse.await(timeout, unit);
        }

        int refreshAttempts() {
            return refreshAttempts.get();
        }

        void releaseProvider() {
            if (providerRelease != null) {
                providerRelease.countDown();
            }
        }

        @Override
        public AuthorizationToken getAccessToken(AuthorizationTokenCommand request) {
            if (refreshAttempts.incrementAndGet() > 1) {
                rejectedSecondUse.countDown();
                throw new IllegalStateException("one-time refresh token was reused");
            }
            then(request.refreshToken()).isEqualTo("one-time-refresh");
            providerCalled.countDown();
            try {
                if (!providerRelease.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("provider release timed out");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("provider call interrupted", interrupted);
            }
            return new AuthorizationToken("fresh-access", "next-refresh", "Bearer", 3600, "scope");
        }

        @Override
        public UserResult getUser(String authorization) {
            throw new AssertionError("refresh must persist the rotated token without a profile lookup");
        }

        @Override
        public void sendMessage(MessageCommand request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribeChatEvent(String sessionKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribeDonationEvent(String sessionKey) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionListResult getSessionList(String clientId, String clientSecret) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionResult getSession(String clientId, String clientSecret) {
            throw new UnsupportedOperationException();
        }
    }
}
