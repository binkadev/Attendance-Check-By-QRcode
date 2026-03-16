ALTER TABLE absence_requests
    MODIFY COLUMN request_status ENUM('PENDING','APPROVED','REJECTED','CANCELLED','REVERTED')
        NOT NULL DEFAULT 'PENDING';

ALTER TABLE absence_requests
    ADD COLUMN reverted_by_user_id BINARY(16) NULL AFTER cancelled_at,
    ADD COLUMN reverted_at DATETIME(3) NULL AFTER reverted_by_user_id,
    ADD COLUMN revert_note VARCHAR(500) NULL AFTER reverted_at,
    ADD COLUMN pending_session_key BINARY(16) NULL AFTER linked_session_id,
    ADD KEY idx_ar_reverted_by_user (reverted_by_user_id),
    ADD UNIQUE KEY uk_ar_requester_pending_session (requester_user_id, pending_session_key),
    ADD KEY idx_ar_session_requester_status (linked_session_id, requester_user_id, request_status),
    ADD KEY idx_ar_group_session_status (group_id, linked_session_id, request_status),
    ADD CONSTRAINT fk_ar_reverted_by
        FOREIGN KEY (reverted_by_user_id) REFERENCES users(id)
            ON UPDATE CASCADE ON DELETE SET NULL;

ALTER TABLE attendance_events
    MODIFY COLUMN event_type ENUM(
        'CHECKIN_QR',
        'MARK_MANUAL_PRESENT',
        'MARK_MANUAL_LATE',
        'MARK_MANUAL_ABSENT',
        'MARK_EXCUSED',
        'REVERT_FROM_EXCUSED',
        'SESSION_OPENED',
        'SESSION_CLOSED',
        'ABSENCE_REQUEST_CREATED',
        'ABSENCE_REQUEST_APPROVED',
        'ABSENCE_REQUEST_REJECTED',
        'ABSENCE_REQUEST_CANCELLED',
        'ABSENCE_REQUEST_REVERTED'
        ) NOT NULL;

DROP TRIGGER IF EXISTS trg_ar_target_xor_ins;
DROP TRIGGER IF EXISTS trg_ar_target_xor_upd;
DROP TRIGGER IF EXISTS trg_ar_status_flow;
DROP TRIGGER IF EXISTS trg_ar_session_only_ins;
DROP TRIGGER IF EXISTS trg_ar_session_only_upd;

DELIMITER $$

CREATE TRIGGER trg_ar_session_only_ins
    BEFORE INSERT ON absence_requests
    FOR EACH ROW
BEGIN
    IF NEW.linked_session_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'linked_session_id is required';
    END IF;

    IF NEW.requested_date IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'requested_date is legacy-only and not allowed for new requests';
    END IF;

    IF NEW.request_status = 'PENDING' THEN
        SET NEW.pending_session_key = NEW.linked_session_id;
    ELSE
        SET NEW.pending_session_key = NULL;
    END IF;
END$$

CREATE TRIGGER trg_ar_session_only_upd
    BEFORE UPDATE ON absence_requests
    FOR EACH ROW
BEGIN
    IF NEW.linked_session_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'linked_session_id is required';
    END IF;

    IF NEW.requested_date IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'requested_date is legacy-only and not allowed for mutable workflow';
    END IF;

    IF OLD.request_status = 'PENDING'
        AND NEW.request_status NOT IN ('PENDING','APPROVED','REJECTED','CANCELLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid status transition from PENDING';
    END IF;

    IF OLD.request_status = 'APPROVED'
        AND NEW.request_status NOT IN ('APPROVED','REVERTED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid status transition from APPROVED';
    END IF;

    IF OLD.request_status = 'REJECTED'
        AND NEW.request_status <> 'REJECTED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid status transition from REJECTED';
    END IF;

    IF OLD.request_status = 'CANCELLED'
        AND NEW.request_status <> 'CANCELLED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid status transition from CANCELLED';
    END IF;

    IF OLD.request_status = 'REVERTED'
        AND NEW.request_status <> 'REVERTED' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid status transition from REVERTED';
    END IF;

    IF NEW.request_status = 'PENDING' THEN
        SET NEW.pending_session_key = NEW.linked_session_id;
    ELSE
        SET NEW.pending_session_key = NULL;
    END IF;
END$$

DELIMITER ;