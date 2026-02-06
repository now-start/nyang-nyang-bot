package org.nowstart.nyangnyangbot.service;

import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LeaderElectionService {

    private final ChzzkConnectionService connectionService;
    private final Clock clock;
    private final Duration leaderTtl;
    private final AtomicReference<Instant> lastHeartbeat;
    private final AtomicBoolean leader;

    public LeaderElectionService(
            ChzzkConnectionService connectionService,
            Clock clock,
            @Value("${leader.ttl:15s}") Duration leaderTtl
    ) {
        this.connectionService = connectionService;
        this.clock = clock;
        this.leaderTtl = leaderTtl;
        this.lastHeartbeat = new AtomicReference<>();
        this.leader = new AtomicBoolean(false);
    }

    @Scheduled(fixedDelayString = "${leader.check-delay:3000}")
    @SchedulerLock(name = "chzzk-leader", lockAtLeastFor = "3s", lockAtMostFor = "15s")
    public void leaderHeartbeat() {
        heartbeat();
    }

    @Scheduled(fixedDelayString = "${leader.check-delay:3000}")
    public void followerCheck() {
        checkLeadership();
    }

    public void heartbeat() {
        lastHeartbeat.set(clock.instant());
        leader.set(true);
        try {
            connectionService.connectIfNeeded();
        } catch (URISyntaxException ex) {
            log.warn("[ChzzkChat][CONNECT_FAILED] {}", ex.getMessage(), ex);
        }
    }

    public void checkLeadership() {
        Instant heartbeat = lastHeartbeat.get();
        if (heartbeat == null) {
            return;
        }

        Instant now = clock.instant();
        if (leader.get() && now.isAfter(heartbeat.plus(leaderTtl))) {
            leader.set(false);
            connectionService.disconnect();
        }
    }

    public boolean isLeader() {
        return leader.get();
    }
}






