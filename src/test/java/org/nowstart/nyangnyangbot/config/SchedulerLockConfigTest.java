package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.BDDAssertions.then;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SchedulerLockConfigTest {

    @Test
    void lockProvider_ShouldCreateProvider() {
        SchedulerLockConfig config = new SchedulerLockConfig();
        DataSource dataSource = Mockito.mock(DataSource.class);

        LockProvider provider = config.lockProvider(dataSource);

        then(provider).isNotNull();
    }

    @Test
    void clock_ShouldReturnSystemClock() {
        SchedulerLockConfig config = new SchedulerLockConfig();

        then(config.clock()).isNotNull();
        then(config.clock().getZone()).isNotNull();
    }
}






