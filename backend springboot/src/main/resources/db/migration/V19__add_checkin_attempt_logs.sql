CREATE TABLE checkin_attempt_logs (
                                      id                BINARY(16) NOT NULL,
                                      group_id          BINARY(16) NOT NULL,
                                      session_id        BINARY(16) NOT NULL,
                                      user_id           BINARY(16) NULL,
                                      qr_token_id       VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
                                      token_hash        VARBINARY(32) NULL,

                                      device_id         VARCHAR(120) CHARACTER SET ascii COLLATE ascii_bin NULL,
                                      ip_address        VARCHAR(45) NULL,
                                      user_agent        VARCHAR(255) NULL,

                                      geo_lat           DECIMAL(10,7) NULL,
                                      geo_lng           DECIMAL(10,7) NULL,
                                      distance_meter    INT UNSIGNED NULL,

                                      outcome           VARCHAR(32) NOT NULL,
                                      failure_code      VARCHAR(40) NULL,

                                      payload_json      JSON NULL,

                                      created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

                                      PRIMARY KEY (id),

                                      KEY idx_cal_created_at (created_at),
                                      KEY idx_cal_session_created (session_id, created_at),
                                      KEY idx_cal_user_created (user_id, created_at),
                                      KEY idx_cal_group_device_created (group_id, device_id, created_at),
                                      KEY idx_cal_group_ip_created (group_id, ip_address, created_at),
                                      KEY idx_cal_outcome_code_created (outcome, failure_code, created_at),
                                      KEY idx_cal_token_hash_created (token_hash, created_at),

                                      CONSTRAINT fk_cal_group
                                          FOREIGN KEY (group_id) REFERENCES class_groups(id)
                                              ON UPDATE CASCADE
                                              ON DELETE RESTRICT,

                                      CONSTRAINT fk_cal_session
                                          FOREIGN KEY (session_id) REFERENCES attendance_sessions(id)
                                              ON UPDATE CASCADE
                                              ON DELETE RESTRICT,

                                      CONSTRAINT fk_cal_user
                                          FOREIGN KEY (user_id) REFERENCES users(id)
                                              ON UPDATE CASCADE
                                              ON DELETE SET NULL,

                                      CONSTRAINT chk_cal_outcome
                                          CHECK (outcome IN ('SUCCESS', 'FAIL')),

                                      CONSTRAINT chk_cal_failure_code_required
                                          CHECK (
                                              (outcome = 'SUCCESS' AND failure_code IS NULL)
                                                  OR
                                              (outcome = 'FAIL' AND failure_code IS NOT NULL)
                                              ),

                                      CONSTRAINT chk_cal_distance_non_negative
                                          CHECK (distance_meter IS NULL OR distance_meter >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
