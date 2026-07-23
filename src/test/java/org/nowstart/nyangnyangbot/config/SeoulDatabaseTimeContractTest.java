package org.nowstart.nyangnyangbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
class SeoulDatabaseTimeContractTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localRuntimeNativeQueriesAndTimestampDefaultsUseSeoulSession() {
        String userId = "seoul-time-contract";
        LocalDateTime beforeDefault = LocalDateTime.now(SEOUL).minusSeconds(2);
        jdbcTemplate.update("INSERT INTO user_account (user_id, is_admin) VALUES (?, FALSE)", userId);
        try {
            LocalDateTime createdAt = jdbcTemplate.queryForObject(
                    "SELECT created_at FROM user_account WHERE user_id = ?",
                    LocalDateTime.class,
                    userId
            );
            LocalDateTime afterDefault = LocalDateTime.now(SEOUL).plusSeconds(2);
            assertThat(createdAt).isBetween(beforeDefault, afterDefault);
            assertThat(objectMapper.getSerializationConfig().getTimeZone().toZoneId()).isEqualTo(SEOUL);
            assertThat(entityManagerFactory.getProperties().get("hibernate.jdbc.time_zone"))
                    .isEqualTo("Asia/Seoul");
            assertThat(entityManagerFactory.getProperties()
                    .get("hibernate.type.preferred_instant_jdbc_type"))
                    .isEqualTo("TIMESTAMP");
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT EXTRACT(TIMEZONE_HOUR FROM CURRENT_TIMESTAMP)",
                    Integer.class
            )).isEqualTo(9);

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
    void instantValuesRoundTripThroughSeoulTimestampSession() {
        Instant expectedInstant = Instant.parse("2026-01-01T15:30:00.123456Z");
        LocalDateTime expectedWallClock = LocalDateTime.of(
                2026, 1, 2, 0, 30, 0, 123_456_000);

        userAccountRepository.saveAndFlush(UserAccount.builder()
                .userId("seoul-jpa-write")
                .admin(false)
                .lastLoginAt(expectedInstant)
                .build());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_login_at FROM user_account WHERE user_id = 'seoul-jpa-write'",
                LocalDateTime.class
        )).isEqualTo(expectedWallClock);

        jdbcTemplate.update("""
                INSERT INTO user_account (user_id, is_admin, last_login_at)
                VALUES ('seoul-jdbc-write', FALSE, '2026-01-02 00:30:00.123456')
                """);
        entityManager.clear();
        assertThat(userAccountRepository.findById("seoul-jdbc-write"))
                .get()
                .extracting(UserAccount::getLastLoginAt)
                .isEqualTo(expectedInstant);
    }
}
