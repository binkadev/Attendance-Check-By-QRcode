CREATE TABLE notification_rule_configs (
                                           id BINARY(16) NOT NULL,
                                           group_id BINARY(16) NULL,
                                           type VARCHAR(50) NOT NULL,
                                           channel VARCHAR(20) NOT NULL,
                                           enabled BIT(1) NOT NULL DEFAULT b'1',
                                           created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                           updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

                                           CONSTRAINT pk_notification_rule_configs PRIMARY KEY (id),
                                           CONSTRAINT uk_nrc_group_type_channel UNIQUE (group_id, type, channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_nrc_lookup
    ON notification_rule_configs (group_id, type, channel, enabled);

CREATE INDEX idx_nrc_type_channel
    ON notification_rule_configs (type, channel);