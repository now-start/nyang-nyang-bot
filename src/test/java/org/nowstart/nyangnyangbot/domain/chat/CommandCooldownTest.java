package org.nowstart.nyangnyangbot.domain.chat;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class CommandCooldownTest {

    @Test
    void isInCooldown_ShouldAllowOnlyFirstConcurrentCommand() throws Exception {
        // 준비
        int workers = 64;
        CommandCooldown cooldown = new CommandCooldown(30_000);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(workers)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return cooldown.isInCooldown("user-1", "command-1", 1_000L);
                }));
            }
            ready.await();

            // 실행
            start.countDown();

            // 검증
            long allowedCount = 0;
            for (Future<Boolean> result : results) {
                if (!result.get()) {
                    allowedCount++;
                }
            }
            then(allowedCount).isEqualTo(1);
        }
    }

    @Test
    void isInCooldown_ShouldAllowCommandAtCooldownBoundary() {
        CommandCooldown cooldown = new CommandCooldown(1_000);

        then(cooldown.isInCooldown("user-1", "command-1", 1_000L)).isFalse();
        then(cooldown.isInCooldown("user-1", "command-1", 1_999L)).isTrue();
        then(cooldown.isInCooldown("user-1", "command-1", 2_000L)).isFalse();
    }
}
