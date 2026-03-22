CREATE TABLE fraud_incidents (
                                 id                     BINARY(16) NOT NULL,
                                 group_id               BINARY(16) NOT NULL,
                                 session_id             BINARY(16) NULL,
                                 user_id                BINARY(16) NULL,

                                 type                   VARCHAR(50) NOT NULL,
                                 severity               VARCHAR(20) NOT NULL,
                                 status                 VARCHAR(20) NOT NULL,

                                 dedup_key              CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,

                                 first_detected_at      DATETIME(3) NOT NULL,
                                 last_detected_at       DATETIME(3) NOT NULL,
                                 occurrence_count       INT UNSIGNED NOT NULL DEFAULT 1,

                                 evidence_json          JSON NULL,

                                 assigned_to_user_id    BINARY(16) NULL,
                                 last_action_by_user_id BINARY(16) NULL,

                                 resolved_at            DATETIME(3) NULL,
                                 resolution_note        VARCHAR(500) NULL,

                                 created_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                 updated_at             DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                                     ON UPDATE CURRENT_TIMESTAMP(3),

                                 PRIMARY KEY (id),

                                 UNIQUE KEY uk_fraud_incidents_dedup_key (dedup_key),

                                 KEY idx_fi_group_status_last_detected (group_id, status, last_detected_at),
                                 KEY idx_fi_group_type_last_detected (group_id, type, last_detected_at),
                                 KEY idx_fi_group_severity_last_detected (group_id, severity, last_detected_at),
                                 KEY idx_fi_assigned_status_last_detected (assigned_to_user_id, status, last_detected_at),
                                 KEY idx_fi_group_created (group_id, created_at),

                                 CONSTRAINT fk_fi_group
                                     FOREIGN KEY (group_id) REFERENCES class_groups(id)
                                         ON UPDATE CASCADE
                                         ON DELETE RESTRICT,

                                 CONSTRAINT fk_fi_session
                                     FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
                                         ON UPDATE CASCADE
                                         ON DELETE SET NULL,

                                 CONSTRAINT fk_fi_user
                                     FOREIGN KEY (user_id) REFERENCES users(id)
                                         ON UPDATE CASCADE
                                         ON DELETE SET NULL,

                                 CONSTRAINT fk_fi_assigned_to
                                     FOREIGN KEY (assigned_to_user_id) REFERENCES users(id)
                                         ON UPDATE CASCADE
                                         ON DELETE SET NULL,

                                 CONSTRAINT fk_fi_last_action_by
                                     FOREIGN KEY (last_action_by_user_id) REFERENCES users(id)
                                         ON UPDATE CASCADE
                                         ON DELETE SET NULL,

                                 CONSTRAINT chk_fi_type
                                     CHECK (type IN (
                                                     'REPEATED_FAILED_QR_TOKEN',
                                                     'WRONG_SESSION_QR_TOKEN',
                                                     'EXPIRED_QR_TOKEN',
                                                     'REPEATED_OUT_OF_RANGE',
                                                     'IP_BURST_MULTI_ATTEMPT',
                                                     'SHARED_DEVICE_MULTI_ACCOUNT'
                                         )),

                                 CONSTRAINT chk_fi_severity
                                     CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),

                                 CONSTRAINT chk_fi_status
                                     CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED', 'FALSE_POSITIVE')),

                                 CONSTRAINT chk_fi_occurrence_count
                                     CHECK (occurrence_count >= 1),

                                 CONSTRAINT chk_fi_resolved_state
                                     CHECK (
                                         (status IN ('OPEN', 'ACKNOWLEDGED') AND resolved_at IS NULL)
                                             OR
                                         (status IN ('RESOLVED', 'FALSE_POSITIVE') AND resolved_at IS NOT NULL)
                                         ),

                                 CONSTRAINT chk_fi_resolution_note_len
                                     CHECK (resolution_note IS NULL OR CHAR_LENGTH(resolution_note) BETWEEN 1 AND 500),

                                 CONSTRAINT chk_fi_detected_order
                                     CHECK (last_detected_at >= first_detected_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
