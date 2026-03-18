CREATE TABLE attendance_policies (
                                     id                    BINARY(16) NOT NULL,
                                     group_id              BINARY(16) NOT NULL,

                                     late_weight           DECIMAL(5,4) NOT NULL DEFAULT 1.0000,

                                     warning_below_rate    DECIMAL(5,2) NOT NULL DEFAULT 85.00,
                                     critical_below_rate   DECIMAL(5,2) NULL,

                                     warning_absent_count  INT NULL,
                                     critical_absent_count INT NULL,

                                     created_by_user_id    BINARY(16) NOT NULL,
                                     updated_by_user_id    BINARY(16) NOT NULL,

                                     created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                     updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                             ON UPDATE CURRENT_TIMESTAMP(3),

                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_ap_group (group_id),
                                     KEY idx_ap_updated_at (updated_at),

                                     CONSTRAINT fk_ap_group
                                         FOREIGN KEY (group_id) REFERENCES class_groups(id)
                                             ON UPDATE CASCADE ON DELETE CASCADE,

                                     CONSTRAINT fk_ap_created_by
                                         FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                                             ON UPDATE CASCADE ON DELETE RESTRICT,

                                     CONSTRAINT fk_ap_updated_by
                                         FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
                                             ON UPDATE CASCADE ON DELETE RESTRICT,

                                     CONSTRAINT chk_ap_late_weight
                                         CHECK (late_weight >= 0.0000 AND late_weight <= 1.0000),

                                     CONSTRAINT chk_ap_warning_rate
                                         CHECK (warning_below_rate >= 0.00 AND warning_below_rate <= 100.00),

                                     CONSTRAINT chk_ap_critical_rate
                                         CHECK (critical_below_rate IS NULL OR (critical_below_rate >= 0.00 AND critical_below_rate <= 100.00)),

                                     CONSTRAINT chk_ap_warning_absent
                                         CHECK (warning_absent_count IS NULL OR warning_absent_count >= 1),

                                     CONSTRAINT chk_ap_critical_absent
                                         CHECK (critical_absent_count IS NULL OR critical_absent_count >= 1),

                                     CONSTRAINT chk_ap_rate_order
                                         CHECK (critical_below_rate IS NULL OR critical_below_rate < warning_below_rate),

                                     CONSTRAINT chk_ap_absent_order
                                         CHECK (
                                             critical_absent_count IS NULL
                                                 OR warning_absent_count IS NULL
                                                 OR critical_absent_count > warning_absent_count
                                             )
) ENGINE=InnoDB;

CREATE INDEX idx_as_group_status_deleted_start
    ON attendance_sessions (group_id, status, deleted_at, start_at);