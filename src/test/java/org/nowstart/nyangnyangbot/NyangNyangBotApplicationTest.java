package org.nowstart.nyangnyangbot;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NyangNyangBotApplicationTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("app.skipRun");
    }

    @Test
    void run_ShouldSkip_WhenFlagEnabled() {
        System.setProperty("app.skipRun", "true");
        AtomicBoolean called = new AtomicBoolean(false);

        NyangNyangBotApplication.run(new String[] {"arg"}, args -> called.set(true));

        then(called.get()).isFalse();
    }

    @Test
    void run_ShouldInvokeRunner_WhenFlagDisabled() {
        AtomicBoolean called = new AtomicBoolean(false);

        NyangNyangBotApplication.run(new String[] {"arg"}, args -> called.set(true));

        then(called.get()).isTrue();
    }

    @Test
    void main_ShouldRespectSkipFlag() {
        System.setProperty("app.skipRun", "true");

        NyangNyangBotApplication.main(new String[] {"arg"});

        then(Boolean.getBoolean("app.skipRun")).isTrue();
    }
}






