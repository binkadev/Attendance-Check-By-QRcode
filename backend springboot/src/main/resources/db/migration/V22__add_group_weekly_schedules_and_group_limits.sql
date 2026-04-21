ALTER TABLE class_groups
    ADD COLUMN total_sessions INT NULL AFTER room,
    ADD COLUMN max_allowed_absences INT NULL AFTER total_sessions;

CREATE TABLE group_weekly_schedules (
                                        id BINARY(16) NOT NULL,
                                        group_id BINARY(16) NOT NULL,
                                        day_of_week VARCHAR(16) NOT NULL,
                                        start_time TIME NOT NULL,
                                        end_time TIME NOT NULL,
                                        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                        updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
                                        deleted_at DATETIME(3) NULL,
                                        PRIMARY KEY (id),
                                        CONSTRAINT fk_group_weekly_schedules_group
                                            FOREIGN KEY (group_id) REFERENCES class_groups(id),
                                        INDEX idx_group_weekly_schedules_group (group_id),
                                        INDEX idx_group_weekly_schedules_group_day (group_id, day_of_week)
);