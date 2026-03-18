package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.service.AttendancePolicyQueryService;
import com.attendance.backend.common.exception.ApiException;
import com.attendance.backend.security.UserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@Validated
public class AttendancePolicyStatusController {

    private final AttendancePolicyQueryService attendancePolicyQueryService;

    public AttendancePolicyStatusController(AttendancePolicyQueryService attendancePolicyQueryService) {
        this.attendancePolicyQueryService = attendancePolicyQueryService;
    }

    @GetMapping("/{groupId}/attendance-policy/students")
    public AttendancePolicyQueryDtos.AttendancePolicyStudentsPageResponse listGroupStudentStatuses(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String sort
    ) {
        UUID actorUserId = requireUserId(me);
        return attendancePolicyQueryService.listGroupStudentStatuses(
                groupId,
                actorUserId,
                q,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{groupId}/me/attendance-policy-status")
    public AttendancePolicyQueryDtos.MyAttendancePolicyStatusResponse getMyPolicyStatus(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal UserPrincipal me
    ) {
        UUID actorUserId = requireUserId(me);
        return attendancePolicyQueryService.getMyPolicyStatus(groupId, actorUserId);
    }

    private UUID requireUserId(UserPrincipal me) {
        if (me == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "Missing JWT principal");
        }
        return me.getUserId();
    }
}