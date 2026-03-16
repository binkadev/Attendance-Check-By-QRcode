ALTER TABLE attendance_sessions
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;

CREATE INDEX idx_attendance_sessions_deleted_at
    ON attendance_sessions (deleted_at);