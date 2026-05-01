ALTER TABLE class_groups
    ADD COLUMN start_date DATE NULL AFTER campus,
    ADD COLUMN planned_end_date DATE NULL AFTER start_date;

CREATE INDEX idx_class_groups_schedule_dates
    ON class_groups (status, deleted_at, start_date, planned_end_date);
