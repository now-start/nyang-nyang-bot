package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.entity.UserAccount;
import org.nowstart.nyangnyangbot.adapter.out.persistence.user.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("local")
class UtcDatabaseTimeContractTest {

    @Autowired
    private org.nowstart.nyangnyangbot.adapter.out.persistence.point.repository.PointLedgerEntryRepository ledger;

    @Test
    @Transactional
    void pointHistoryPreservesMicrosecondsAndBalanceBeforeLimit() {
        jdbcTemplate.update("INSERT INTO user_account (user_id, is_admin) VALUES ('history-contract', FALSE)");
        jdbcTemplate.update("""
                INSERT INTO point_ledger_entry
                    (user_id, delta, source_type, description, idempotency_key, created_at)
                VALUES ('history-contract', 10, 'REWARD_MANUAL', 'first', 'history-first',
                        '2026-11-18 23:59:59.123456+09'),
                       ('history-contract', -3, 'REWARD_MANUAL', 'second', 'history-second',
                        '2026-11-19 00:00:00.654321+09')
                """);
        var history = ledger.findHistory("history-contract", 1);
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getBalanceAfter()).isEqualTo(7L);
        assertThat(history.getFirst().getDelta()).isEqualTo(-3L);
        assertThat(history.getFirst().getCreatedAt()).isEqualTo(Instant.parse("2026-11-18T15:00:00.654321Z"));
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void utcStorageAndSeoulDisplayAreIndependent() {
        String userId = "seoul-time-contract";
        LocalDateTime beforeDefault = LocalDateTime.now(SEOUL).minusSeconds(2);
        jdbcTemplate.update("INSERT INTO user_account (user_id, is_admin) VALUES (?, FALSE)", userId);
        try {
            LocalDateTime createdAt = jdbcTemplate.queryForObject(
                    "SELECT created_at AT TIME ZONE 'Asia/Seoul' FROM user_account WHERE user_id = ?",
                    LocalDateTime.class,
                    userId
            );
            LocalDateTime afterDefault = LocalDateTime.now(SEOUL).plusSeconds(2);
            assertThat(createdAt).isBetween(beforeDefault, afterDefault);
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

    @Test
    @Transactional
    void instantValuesRoundTripThroughUtcSession() {
        Instant expectedInstant = Instant.parse("2026-01-01T15:30:00.123456Z");
        LocalDateTime expectedWallClock = LocalDateTime.of(
                2026, 1, 2, 0, 30, 0, 123_456_000);

        userAccountRepository.saveAndFlush(UserAccount.builder()
                .userId("seoul-jpa-write")
                .admin(false)
                .lastLoginAt(expectedInstant)
                .build());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_login_at AT TIME ZONE 'Asia/Seoul' FROM user_account WHERE user_id = 'seoul-jpa-write'",
                LocalDateTime.class
        )).isEqualTo(expectedWallClock);

        jdbcTemplate.update("""
                INSERT INTO user_account (user_id, is_admin, last_login_at)
                VALUES ('seoul-jdbc-write', FALSE, '2026-01-02 00:30:00.123456+09:00')
                """);
        entityManager.clear();
        assertThat(userAccountRepository.findById("seoul-jdbc-write"))
                .get()
                .extracting(UserAccount::getLastLoginAt)
                .isEqualTo(expectedInstant);
    }

    @Test
    @Transactional
    void observingUserOnlyUpdatesTimestampWhenDisplayNameChanges() {
        String userId = "observe-user-timestamp";
        LocalDateTime originalUpdatedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        jdbcTemplate.update("""
                INSERT INTO user_account (user_id, display_name, is_admin, created_at, updated_at)
                VALUES (?, ?, FALSE, ?, ?)
                """, userId, "same-name", originalUpdatedAt.atZone(SEOUL).toOffsetDateTime(),
                originalUpdatedAt.atZone(SEOUL).toOffsetDateTime());

        userAccountRepository.observe(userId, null);
        userAccountRepository.observe(userId, "");
        userAccountRepository.observe(userId, "   ");
        userAccountRepository.observe(userId, "same-name");

        assertThat(updatedAt(userId)).isEqualTo(originalUpdatedAt);

        userAccountRepository.observe(userId, "changed-name");

        assertThat(updatedAt(userId)).isAfter(originalUpdatedAt);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT display_name FROM user_account WHERE user_id = ?",
                String.class,
                userId
        )).isEqualTo("changed-name");

        LocalDateTime beforeCaseChange = updatedAt(userId);
        userAccountRepository.observe(userId, "CHANGED-NAME");
        assertThat(updatedAt(userId)).isAfter(beforeCaseChange);

    }

    private LocalDateTime updatedAt(String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at AT TIME ZONE 'Asia/Seoul' FROM user_account WHERE user_id = ?",
                LocalDateTime.class,
                userId
        );
    }
}
