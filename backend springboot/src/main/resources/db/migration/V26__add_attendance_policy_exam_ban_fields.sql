ALTER TABLE attendance_policies
    MODIFY COLUMN warning_below_rate DECIMAL(5,2) NOT NULL DEFAULT 80.00,
    MODIFY COLUMN critical_below_rate DECIMAL(5,2) NULL DEFAULT 75.00,
    ADD COLUMN exam_ban_absence_rate DECIMAL(5,2) NULL AFTER critical_below_rate,
    ADD COLUMN exam_ban_absent_count INT NULL AFTER critical_absent_count,
    ADD CONSTRAINT chk_ap_exam_ban_absence_rate
        CHECK (exam_ban_absence_rate IS NULL OR (exam_ban_absence_rate >= 0.00 AND exam_ban_absence_rate <= 100.00)),
    ADD CONSTRAINT chk_ap_exam_ban_absent
        CHECK (exam_ban_absent_count IS NULL OR exam_ban_absent_count >= 1),
    ADD CONSTRAINT chk_ap_exam_ban_absent_order
        CHECK (
            exam_ban_absent_count IS NULL
                OR critical_absent_count IS NULL
                OR exam_ban_absent_count > critical_absent_count
            );
