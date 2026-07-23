package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class UtcDatabaseTimeContractTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void localRuntimeNativeQueriesAndColumnDefaultsUseUtcWallClock() {
        String userId = "utc-time-contract";
        LocalDateTime beforeDefault = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(2);
        jdbcTemplate.update("INSERT INTO user_account (user_id, is_admin) VALUES (?, FALSE)", userId);
        try {
            LocalDateTime createdAt = jdbcTemplate.queryForObject(
                    "SELECT created_at FROM user_account WHERE user_id = ?",
                    LocalDateTime.class,
                    userId
            );
            LocalDateTime afterDefault = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(2);
            assertThat(createdAt).isBetween(beforeDefault, afterDefault);
            assertThat(TimeZone.getDefault().getRawOffset()).isZero();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT EXTRACT(TIMEZONE_HOUR FROM CURRENT_TIMESTAMP)",
                    Integer.class
            )).isZero();

            Instant beforeNative = Instant.now().minusSeconds(2);
            Instant databaseNow = userAccountRepository.currentDatabaseTime();
            Instant afterNative = Instant.now().plusSeconds(2);
            assertThat(databaseNow).isBetween(beforeNative, afterNative);
        } finally {
            jdbcTemplate.update("DELETE FROM user_account WHERE user_id = ?", userId);
        }
    }
}
