CREATE TABLE notifications (
                               id                  BINARY(16) NOT NULL,
                               recipient_user_id   BINARY(16) NOT NULL,
                               group_id            BINARY(16) NULL,
                               session_id          BINARY(16) NULL,

                               type                VARCHAR(50) NOT NULL,
                               title               VARCHAR(200) NOT NULL,
                               body                VARCHAR(1000) NOT NULL,
                               payload_json        JSON NULL,

                               severity            VARCHAR(20) NOT NULL,

                               source_type         VARCHAR(50) NULL,
                               source_ref_id       BINARY(16) NULL,

                               dedup_key           CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,

                               is_read             TINYINT NOT NULL DEFAULT 0,
                               read_at             DATETIME(3) NULL,

                               created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                               updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

                               PRIMARY KEY (id),

                               UNIQUE KEY uk_notifications_dedup_key (dedup_key),

                               KEY idx_notifications_recipient_created
                                   (recipient_user_id, created_at),

                               KEY idx_notifications_recipient_unread_created
                                   (recipient_user_id, is_read, created_at),

                               KEY idx_notifications_group_recipient_created
                                   (group_id, recipient_user_id, created_at),

                               KEY idx_notifications_source
                                   (source_type, source_ref_id),

                               KEY idx_notifications_session_created
                                   (session_id, created_at),

                               CONSTRAINT fk_notifications_recipient
                                   FOREIGN KEY (recipient_user_id) REFERENCES users(id)
                                       ON UPDATE CASCADE
                                       ON DELETE RESTRICT,

                               CONSTRAINT fk_notifications_group
                                   FOREIGN KEY (group_id) REFERENCES class_groups(id)
                                       ON UPDATE CASCADE
                                       ON DELETE SET NULL,

                               CONSTRAINT fk_notifications_session
                                   FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
                                       ON UPDATE CASCADE
                                       ON DELETE SET NULL,

                               CONSTRAINT chk_notifications_title_len
                                   CHECK (CHAR_LENGTH(title) BETWEEN 1 AND 200),

                               CONSTRAINT chk_notifications_body_len
                                   CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 1000),

                               CONSTRAINT chk_notifications_severity
                                   CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),

                               CONSTRAINT chk_notifications_read_state
                                   CHECK (
                                       (is_read = 0 AND read_at IS NULL)
                                           OR
                                       (is_read = 1 AND read_at IS NOT NULL)
                                       )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;