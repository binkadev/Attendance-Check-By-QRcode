CREATE TABLE notification_deliveries (
                                         id                  BINARY(16) NOT NULL,
                                         notification_id     BINARY(16) NOT NULL,

                                         channel             VARCHAR(20) NOT NULL,
                                         status              VARCHAR(20) NOT NULL,

                                         email_outbox_id     BINARY(16) NULL,

                                         retry_count         INT NOT NULL DEFAULT 0,
                                         next_attempt_at     DATETIME(3) NOT NULL,

                                         locked_at           DATETIME(3) NULL,
                                         processed_at        DATETIME(3) NULL,

                                         last_error_code     VARCHAR(50) NULL,
                                         last_error_message  VARCHAR(500) NULL,

                                         created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                                         updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),

                                         PRIMARY KEY (id),

                                         UNIQUE KEY uk_notification_deliveries_notification_channel
                                             (notification_id, channel),

                                         KEY idx_nd_claim
                                             (channel, status, next_attempt_at, email_outbox_id, locked_at),

                                         KEY idx_nd_admin_status_created
                                             (status, created_at),

                                         KEY idx_nd_email_outbox
                                             (email_outbox_id),

                                         KEY idx_nd_notification_created
                                             (notification_id, created_at),

                                         CONSTRAINT fk_nd_notification
                                             FOREIGN KEY (notification_id) REFERENCES notifications(id)
                                                 ON UPDATE CASCADE
                                                 ON DELETE CASCADE,

                                         CONSTRAINT fk_nd_email_outbox
                                             FOREIGN KEY (email_outbox_id) REFERENCES email_outbox(id)
                                                 ON UPDATE CASCADE
                                                 ON DELETE SET NULL,

                                         CONSTRAINT chk_nd_retry_count
                                             CHECK (retry_count >= 0),

                                         CONSTRAINT chk_nd_channel
                                             CHECK (channel IN ('EMAIL', 'PUSH', 'WEBSOCKET')),

                                         CONSTRAINT chk_nd_status
                                             CHECK (status IN ('PENDING', 'PROCESSING', 'ENQUEUED', 'RETRY', 'DELIVERED', 'DEAD'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;