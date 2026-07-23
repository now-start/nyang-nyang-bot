-- nyang-nyang-bot target database invariants
-- Dialect: MariaDB 10.11
-- Apply after target-schema.sql. Flyway should execute the compound trigger bodies
-- through its MariaDB parser or a Java migration; DELIMITER is a mariadb-client command.

DELIMITER //

CREATE TRIGGER trg_point_ledger_entry__reject_update
BEFORE UPDATE ON point_ledger_entry
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'point_ledger_entry is insert-only; create a correction entry';
END//

CREATE TRIGGER trg_point_ledger_entry__reject_delete
BEFORE DELETE ON point_ledger_entry
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'point_ledger_entry is insert-only and cannot be deleted';
END//

-- The application treats command_execution as append-only. DELETE intentionally
-- remains available only for an operator's direct database count reset.
CREATE TRIGGER trg_command_execution__guard_insert
BEFORE INSERT ON command_execution
FOR EACH ROW
BEGIN
    IF NEW.execution_policy_snapshot = 'USER_CALENDAR_DAY'
       AND NOT (NEW.calendar_date <=> DATE(DATE_ADD(NEW.executed_at, INTERVAL 9 HOUR))) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'command_execution calendar_date must match the UTC approval time in Asia/Seoul';
    END IF;
END//

CREATE TRIGGER trg_command_execution__reject_update
BEFORE UPDATE ON command_execution
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'command_execution rows cannot be updated; operator resets use DELETE';
END//

CREATE TRIGGER trg_donation__reject_update
BEFORE UPDATE ON donation
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'donation is an immutable source event';
END//

CREATE TRIGGER trg_donation__reject_delete
BEFORE DELETE ON donation
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'donation cannot be deleted';
END//

CREATE TRIGGER trg_roulette_config__draft_insert
BEFORE INSERT ON roulette_config
FOR EACH ROW
BEGIN
    IF NEW.status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_config must be inserted as DRAFT';
    END IF;
END//

CREATE TRIGGER trg_roulette_config__guard_update
BEFORE UPDATE ON roulette_config
FOR EACH ROW
BEGIN
    DECLARE probability_total BIGINT DEFAULT 0;
    DECLARE positive_losing_count BIGINT DEFAULT 0;

    IF NEW.id <> OLD.id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_config.id is immutable';
    END IF;

    IF NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_config.created_at is immutable';
    END IF;

    IF OLD.status = 'ARCHIVED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ARCHIVED roulette_config is immutable';
    END IF;

    IF OLD.status = 'ACTIVE' AND (
        NEW.status <> 'ARCHIVED'
        OR NOT (NEW.title <=> OLD.title)
        OR NOT (NEW.trigger_token <=> OLD.trigger_token)
        OR NOT (NEW.price_per_round <=> OLD.price_per_round)
        OR NOT (NEW.high_round_threshold <=> OLD.high_round_threshold)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'ACTIVE roulette_config can only transition to ARCHIVED';
    END IF;

    IF OLD.status = 'DRAFT' AND NEW.status = 'ACTIVE' THEN
        SELECT
            COALESCE(SUM(probability_basis_points), 0),
            COALESCE(SUM(
                CASE WHEN is_losing = TRUE AND probability_basis_points > 0 THEN 1 ELSE 0 END
            ), 0)
        INTO probability_total, positive_losing_count
        FROM roulette_option
        WHERE roulette_config_id = OLD.id
        FOR UPDATE;

        IF probability_total <> 10000 OR positive_losing_count = 0 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'ACTIVE roulette_config requires probability 10000 and a positive losing option';
        END IF;
    END IF;
END//

CREATE TRIGGER trg_roulette_config__guard_delete
BEFORE DELETE ON roulette_config
FOR EACH ROW
BEGIN
    IF OLD.status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'non-DRAFT roulette_config cannot be deleted';
    END IF;
END//

CREATE TRIGGER trg_roulette_option__draft_insert
BEFORE INSERT ON roulette_option
FOR EACH ROW
BEGIN
    DECLARE config_status VARCHAR(16) DEFAULT NULL;

    SELECT status INTO config_status
    FROM roulette_config
    WHERE id = NEW.roulette_config_id
    FOR UPDATE;

    IF config_status IS NULL OR config_status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_option can only be inserted into a DRAFT config';
    END IF;
END//

CREATE TRIGGER trg_roulette_option__guard_update
BEFORE UPDATE ON roulette_option
FOR EACH ROW
BEGIN
    DECLARE config_status VARCHAR(16) DEFAULT NULL;

    IF NEW.id <> OLD.id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_option.id is immutable';
    END IF;

    IF NEW.roulette_config_id <> OLD.roulette_config_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_option cannot move between configs';
    END IF;

    IF NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_option.created_at is immutable';
    END IF;

    SELECT status INTO config_status
    FROM roulette_config
    WHERE id = OLD.roulette_config_id
    FOR UPDATE;

    IF config_status IS NULL OR config_status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'non-DRAFT roulette_option is immutable';
    END IF;
END//

CREATE TRIGGER trg_roulette_option__guard_delete
BEFORE DELETE ON roulette_option
FOR EACH ROW
BEGIN
    DECLARE config_status VARCHAR(16) DEFAULT NULL;

    SELECT status INTO config_status
    FROM roulette_config
    WHERE id = OLD.roulette_config_id
    FOR UPDATE;

    IF config_status IS NULL OR config_status <> 'DRAFT' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'non-DRAFT roulette_option cannot be deleted';
    END IF;
END//

CREATE TRIGGER trg_roulette_run__guard_insert
BEFORE INSERT ON roulette_run
FOR EACH ROW
BEGIN
    DECLARE config_status VARCHAR(16) DEFAULT NULL;
    DECLARE probability_total BIGINT DEFAULT 0;
    DECLARE positive_losing_count BIGINT DEFAULT 0;

    SELECT status INTO config_status
    FROM roulette_config
    WHERE id = NEW.roulette_config_id
    FOR UPDATE;

    SELECT
        COALESCE(SUM(probability_basis_points), 0),
        COALESCE(SUM(
            CASE WHEN is_losing = TRUE AND probability_basis_points > 0 THEN 1 ELSE 0 END
        ), 0)
    INTO probability_total, positive_losing_count
    FROM roulette_option
    WHERE roulette_config_id = NEW.roulette_config_id
    FOR UPDATE;

    IF config_status IS NULL OR config_status <> 'ACTIVE' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run requires an ACTIVE config';
    END IF;

    IF NEW.status <> 'BUILDING' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run must be inserted as BUILDING';
    END IF;

    IF probability_total <> 10000 OR positive_losing_count = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run requires a complete immutable option set';
    END IF;
END//

CREATE TRIGGER trg_roulette_run__guard_update
BEFORE UPDATE ON roulette_run
FOR EACH ROW
BEGIN
    DECLARE expected_round_count BIGINT DEFAULT 0;
    DECLARE total_count BIGINT DEFAULT 0;
    DECLARE distinct_round_count BIGINT DEFAULT 0;
    DECLARE minimum_round_no INTEGER DEFAULT 0;
    DECLARE maximum_round_no INTEGER DEFAULT 0;
    DECLARE non_confirmed_count BIGINT DEFAULT 0;

    IF NEW.donation_id <> OLD.donation_id
        OR NEW.roulette_config_id <> OLD.roulette_config_id
        OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run identity and config are immutable';
    END IF;

    IF OLD.status <> 'BUILDING' OR NEW.status <> 'READY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run only supports BUILDING to READY';
    END IF;

    SELECT FLOOR(donation.amount / config.price_per_round)
    INTO expected_round_count
    FROM donation
    JOIN roulette_config config ON config.id = OLD.roulette_config_id
    WHERE donation.id = OLD.donation_id
    FOR UPDATE;

    SELECT
        COUNT(*),
        COUNT(DISTINCT round_no),
        COALESCE(MIN(round_no), 0),
        COALESCE(MAX(round_no), 0),
        COALESCE(SUM(CASE WHEN status <> 'CONFIRMED' THEN 1 ELSE 0 END), 0)
    INTO total_count, distinct_round_count, minimum_round_no, maximum_round_no,
        non_confirmed_count
    FROM roulette_round
    WHERE roulette_run_id = OLD.donation_id
    FOR UPDATE;

    IF expected_round_count < 1
        OR total_count <> expected_round_count
        OR distinct_round_count <> total_count
        OR minimum_round_no <> 1
        OR maximum_round_no <> expected_round_count
        OR non_confirmed_count <> 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_run requires the complete confirmed round sequence before READY';
    END IF;

END//

CREATE TRIGGER trg_roulette_run__reject_delete
BEFORE DELETE ON roulette_run
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'roulette_run cannot be deleted';
END//

CREATE TRIGGER trg_roulette_round__guard_insert
BEFORE INSERT ON roulette_round
FOR EACH ROW
BEGIN
    DECLARE run_status VARCHAR(24) DEFAULT NULL;

    SELECT status INTO run_status
    FROM roulette_run
    WHERE donation_id = NEW.roulette_run_id
    FOR UPDATE;

    IF run_status IS NULL OR run_status <> 'BUILDING' OR NEW.status <> 'CONFIRMED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_round must be inserted as CONFIRMED while run is BUILDING';
    END IF;
END//

CREATE TRIGGER trg_roulette_round__guard_update
BEFORE UPDATE ON roulette_round
FOR EACH ROW
BEGIN
    DECLARE run_status VARCHAR(24) DEFAULT NULL;

    SELECT status INTO run_status
    FROM roulette_run
    WHERE donation_id = OLD.roulette_run_id
    FOR UPDATE;

    IF run_status IS NULL OR run_status <> 'READY' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_round can only be processed after run is READY';
    END IF;

    IF NEW.id <> OLD.id
        OR NEW.roulette_run_id <> OLD.roulette_run_id
        OR NEW.roulette_config_id <> OLD.roulette_config_id
        OR NEW.roulette_option_id <> OLD.roulette_option_id
        OR NEW.round_no <> OLD.round_no
        OR NEW.ticket <> OLD.ticket
        OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_round draw facts are immutable';
    END IF;

    IF NEW.status <> OLD.status AND (
        OLD.status <> 'CONFIRMED' OR NEW.status NOT IN ('APPLIED', 'FAILED')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'roulette_round status transition is invalid';
    END IF;

    IF OLD.status <> 'CONFIRMED'
        AND NOT (NEW.failure_reason <=> OLD.failure_reason) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'terminal roulette_round failure reason is immutable';
    END IF;
END//

CREATE TRIGGER trg_roulette_round__reject_delete
BEFORE DELETE ON roulette_round
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'roulette_round cannot be deleted';
END//

CREATE TRIGGER trg_reward_grant__guard_update
BEFORE UPDATE ON reward_grant
FOR EACH ROW
BEGIN
    IF NEW.id <> OLD.id
        OR NEW.user_id <> OLD.user_id
        OR NOT (NEW.roulette_round_id <=> OLD.roulette_round_id)
        OR NOT (NEW.point_ledger_entry_id <=> OLD.point_ledger_entry_id)
        OR NOT (NEW.label <=> OLD.label)
        OR NEW.reward_type <> OLD.reward_type
        OR NEW.conversion_mode <> OLD.conversion_mode
        OR NOT (NEW.point_delta <=> OLD.point_delta)
        OR NOT (NEW.description <=> OLD.description)
        OR NOT (NEW.actor_user_id <=> OLD.actor_user_id)
        OR NEW.idempotency_key <> OLD.idempotency_key
        OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'reward_grant identity and granted facts are immutable';
    END IF;

    IF NEW.status <> OLD.status AND NOT (
        (OLD.status = 'OWNED' AND NEW.status = 'USED')
        OR (OLD.status = 'CONVERTED' AND NEW.status = 'CORRECTED')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'reward_grant status transition is invalid';
    END IF;

    SET NEW.updated_at = CURRENT_TIMESTAMP(6);
END//

CREATE TRIGGER trg_reward_grant__reject_delete
BEFORE DELETE ON reward_grant
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'reward_grant cannot be deleted';
END//

CREATE TRIGGER trg_overlay_access_token__guard_update
BEFORE UPDATE ON overlay_access_token
FOR EACH ROW
BEGIN
    IF NEW.id <> OLD.id
        OR NEW.token_hash <> OLD.token_hash
        OR NOT (NEW.issued_by_user_id <=> OLD.issued_by_user_id)
        OR NOT (NEW.issued_at <=> OLD.issued_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'overlay_access_token issuance facts are immutable';
    END IF;

    IF OLD.revoked_at IS NOT NULL
        AND NOT (NEW.revoked_at <=> OLD.revoked_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'overlay_access_token revocation is irreversible';
    END IF;
END//

CREATE TRIGGER trg_overlay_access_token__reject_delete
BEFORE DELETE ON overlay_access_token
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'overlay_access_token cannot be deleted';
END//

CREATE TRIGGER trg_overlay_display_job__guard_update
BEFORE UPDATE ON overlay_display_job
FOR EACH ROW
BEGIN
    IF NEW.id <> OLD.id
        OR NEW.roulette_run_id <> OLD.roulette_run_id
        OR NOT (NEW.replay_of_job_id <=> OLD.replay_of_job_id)
        OR NEW.idempotency_key <> OLD.idempotency_key
        OR NOT (NEW.expires_at <=> OLD.expires_at)
        OR NOT (NEW.created_at <=> OLD.created_at) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'overlay_display_job identity and enqueue facts are immutable';
    END IF;

    IF NOT (
        (OLD.status = 'PENDING' AND NEW.status IN ('DISPLAYING', 'MISSED'))
        OR (OLD.status = 'DISPLAYING'
            AND NEW.status IN ('PENDING', 'DISPLAYING', 'DISPLAYED', 'MISSED'))
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'overlay_display_job status transition is invalid';
    END IF;

    SET NEW.updated_at = CURRENT_TIMESTAMP(6);
END//

CREATE TRIGGER trg_overlay_display_job__reject_delete
BEFORE DELETE ON overlay_display_job
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'overlay_display_job cannot be deleted';
END//

DELIMITER ;
