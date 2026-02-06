package org.nowstart.nyangnyangbot.service;

import static org.assertj.core.api.BDDAssertions.then;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LeaderElectionServiceTest {

    private static final Duration LEADER_TTL = Duration.ofSeconds(15);

    @Mock
    private ChzzkConnectionService connectionService;

    private TestClock clock;
    private LeaderElectionService leaderElectionService;

    @BeforeEach
    void setUp() {
        clock = new TestClock(Instant.parse("2025-01-01T00:00:00Z"));
        leaderElectionService = new LeaderElectionService(connectionService, clock, LEADER_TTL);
    }

    @Test
    void heartbeat_ShouldActivateLeaderAndConnect() throws java.net.URISyntaxException {
        // when
        leaderElectionService.heartbeat();

        // then
        then(leaderElectionService.isLeader()).isTrue();
        BDDMockito.then(connectionService).should().connectIfNeeded();
    }

    @Test
    void checkLeadership_ShouldNotDisconnect_WhenHeartbeatFresh() throws java.net.URISyntaxException {
        // given
        leaderElectionService.heartbeat();
        clock.advanceSeconds(10);

        // when
        leaderElectionService.checkLeadership();

        // then
        then(leaderElectionService.isLeader()).isTrue();
        BDDMockito.then(connectionService).should().connectIfNeeded();
        BDDMockito.then(connectionService).shouldHaveNoMoreInteractions();
    }

    @Test
    void checkLeadership_ShouldDisconnect_WhenHeartbeatStale() {
        // given
        leaderElectionService.heartbeat();
        clock.advanceSeconds(16);

        // when
        leaderElectionService.checkLeadership();

        // then
        then(leaderElectionService.isLeader()).isFalse();
        BDDMockito.then(connectionService).should().disconnect();
    }

    @Test
    void checkLeadership_ShouldDoNothing_WhenHeartbeatMissing() {
        leaderElectionService.checkLeadership();

        then(leaderElectionService.isLeader()).isFalse();
        BDDMockito.then(connectionService).shouldHaveNoInteractions();
    }

    @Test
    void checkLeadership_ShouldNotDisconnect_WhenLeaderFalse() throws java.net.URISyntaxException {
        leaderElectionService.heartbeat();
        clock.advanceSeconds(16);

        AtomicBoolean leaderFlag = new AtomicBoolean(false);
        ReflectionTestUtils.setField(leaderElectionService, "leader", leaderFlag);

        leaderElectionService.checkLeadership();

        then(leaderElectionService.isLeader()).isFalse();
        BDDMockito.then(connectionService).should().connectIfNeeded();
        BDDMockito.then(connectionService).shouldHaveNoMoreInteractions();
    }

    @Test
    void heartbeat_ShouldSwallowConnectionErrors() throws java.net.URISyntaxException {
        BDDMockito.willThrow(new java.net.URISyntaxException("bad", "bad")).given(connectionService).connectIfNeeded();

        leaderElectionService.heartbeat();

        then(leaderElectionService.isLeader()).isTrue();
    }

    @Test
    void leaderHeartbeat_ShouldInvokeHeartbeat() throws java.net.URISyntaxException {
        leaderElectionService.leaderHeartbeat();

        BDDMockito.then(connectionService).should().connectIfNeeded();
    }

    @Test
    void followerCheck_ShouldInvokeCheckLeadership() throws java.net.URISyntaxException {
        leaderElectionService.heartbeat();
        clock.advanceSeconds(16);

        leaderElectionService.followerCheck();

        BDDMockito.then(connectionService).should().disconnect();
        then(leaderElectionService.isLeader()).isFalse();
    }

    private static final class TestClock extends Clock {
        private Instant instant;

        private TestClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}






