package com.attendance.backend.group.exception;

import com.attendance.backend.group.dto.MemberImportValidationError;

import java.util.List;

public class MemberImportValidationException extends RuntimeException {

    private final String code;
    private final List<MemberImportValidationError> errors;

    public MemberImportValidationException(String message, List<MemberImportValidationError> errors) {
        super(message);
        this.code = "MEMBER_IMPORT_VALIDATION_FAILED";
        this.errors = List.copyOf(errors);
    }

    public String getCode() {
        return code;
    }

    public List<MemberImportValidationError> getErrors() {
        return errors;
    }
}
