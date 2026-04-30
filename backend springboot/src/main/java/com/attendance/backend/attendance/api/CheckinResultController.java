package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.dto.CheckinResultResponse;
import com.attendance.backend.attendance.service.CheckinResultService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.security.UserPrincipal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CheckinResultController {

    private final CheckinResultService checkinResultService;

    public CheckinResultController(CheckinResultService checkinResultService) {
        this.checkinResultService = checkinResultService;
    }

    @GetMapping("/sessions/{sessionId}/me/checkin-result")
    public ResponseEntity<CheckinResultResponse> getMyCheckinResult(
            @AuthenticationPrincipal UserPrincipal me,
            @PathVariable UUID sessionId
    ) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }

        CheckinResultResponse response = checkinResultService.getMyCheckinResult(
                me.getUserId(),
                sessionId
        );

        /*
         * Màn kết quả sau QR cần dữ liệu mới nhất sau reset/quét lại.
         * Không cho client/proxy cache GET này.
         */
        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(response);
    }
}