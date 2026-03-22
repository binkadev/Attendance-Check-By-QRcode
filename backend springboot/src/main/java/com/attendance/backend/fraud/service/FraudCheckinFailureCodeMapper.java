package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import org.springframework.stereotype.Component;

@Component
public class FraudCheckinFailureCodeMapper {

    public CheckinFailureCode map(ApiException ex) {
        if (ex == null || ex.getCode() == null || ex.getCode().isBlank()) {
            return CheckinFailureCode.UNKNOWN;
        }

        String code = ex.getCode().trim().toUpperCase();

        return switch (code) {
            case "TOKEN_INVALID", "QR_TOKEN_INVALID", "INVALID_TOKEN" -> CheckinFailureCode.TOKEN_INVALID;
            case "TOKEN_MALFORMED", "QR_TOKEN_MALFORMED", "MALFORMED_TOKEN" -> CheckinFailureCode.TOKEN_MALFORMED;
            case "TOKEN_NOT_FOUND", "QR_TOKEN_NOT_FOUND" -> CheckinFailureCode.TOKEN_NOT_FOUND;
            case "TOKEN_EXPIRED", "QR_TOKEN_EXPIRED" -> CheckinFailureCode.TOKEN_EXPIRED;
            case "TOKEN_WRONG_SESSION", "QR_TOKEN_WRONG_SESSION" -> CheckinFailureCode.TOKEN_WRONG_SESSION;

            case "CHECKIN_NOT_OPEN_YET" -> CheckinFailureCode.CHECKIN_NOT_OPEN_YET;
            case "CHECKIN_CLOSED" -> CheckinFailureCode.CHECKIN_CLOSED;

            case "OUT_OF_RANGE", "CHECKIN_OUT_OF_RANGE", "LOCATION_OUT_OF_RANGE" -> CheckinFailureCode.OUT_OF_RANGE;

            case "DUPLICATE_CHECKIN", "ALREADY_CHECKED_IN" -> CheckinFailureCode.DUPLICATE_CHECKIN;

            case "USER_NOT_MEMBER", "GROUP_MEMBER_NOT_FOUND" -> CheckinFailureCode.USER_NOT_MEMBER;
            case "USER_NOT_APPROVED", "GROUP_MEMBER_NOT_APPROVED" -> CheckinFailureCode.USER_NOT_APPROVED;

            case "SESSION_NOT_FOUND", "ATTENDANCE_SESSION_NOT_FOUND" -> CheckinFailureCode.SESSION_NOT_FOUND;
            case "SESSION_NOT_OPEN", "ATTENDANCE_SESSION_NOT_OPEN" -> CheckinFailureCode.SESSION_NOT_OPEN;

            default -> CheckinFailureCode.UNKNOWN;
        };
    }
}