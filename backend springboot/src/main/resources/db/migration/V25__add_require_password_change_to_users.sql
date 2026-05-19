ALTER TABLE users
    ADD COLUMN require_password_change TINYINT NOT NULL DEFAULT 0 AFTER primary_device_id,
    ADD CONSTRAINT chk_users_require_password_change CHECK (require_password_change IN (0, 1));
