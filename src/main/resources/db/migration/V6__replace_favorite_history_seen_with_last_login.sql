ALTER TABLE authorization_account DROP COLUMN IF EXISTS favorite_history_last_seen_at;

ALTER TABLE authorization_account ADD COLUMN IF NOT EXISTS last_login_at DATETIME(6);
