package com.attendance.backend.common.exception;

import com.attendance.backend.group.exception.MemberImportValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handle(ApiException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), req.getRequestURI()));
    }


    @ExceptionHandler(MemberImportValidationException.class)
    public ResponseEntity<MemberImportValidationErrorResponse> handleMemberImport(
            MemberImportValidationException ex,
            HttpServletRequest req
    ) {
        return ResponseEntity.badRequest()
                .body(new MemberImportValidationErrorResponse(
                        ex.getCode(),
                        ex.getMessage(),
                        req.getRequestURI(),
                        ex.getErrors()
                ));
    }

    public record ApiErrorResponse(String code, String message, String path) {}

    public record MemberImportValidationErrorResponse(
            String code,
            String message,
            String path,
            List<?> errors
    ) {}
}

