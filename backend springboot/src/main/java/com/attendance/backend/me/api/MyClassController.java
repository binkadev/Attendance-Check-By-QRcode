package com.attendance.backend.me.api;

import com.attendance.backend.me.dto.MyClassSemesterOptionResponse;
import com.attendance.backend.me.dto.MyClassTimelineResponse;
import com.attendance.backend.me.dto.PageMyClassResponse;
import com.attendance.backend.me.model.MyClassQueryCriteria;
import com.attendance.backend.me.service.MyClassQueryService;
import com.attendance.backend.me.service.MyClassTimelineService;
import com.attendance.backend.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class MyClassController {

    private final MyClassQueryService myClassQueryService;
    private final MyClassTimelineService myClassTimelineService;

    public MyClassController(
            MyClassQueryService myClassQueryService,
            MyClassTimelineService myClassTimelineService
    ) {
        this.myClassQueryService = myClassQueryService;
        this.myClassTimelineService = myClassTimelineService;
    }

    @GetMapping("/classes")
    public PageMyClassResponse listMyClasses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String memberStatus,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        MyClassQueryCriteria criteria = MyClassQueryCriteria.fromRequest(
                q,
                scope,
                status,
                memberStatus,
                semester,
                academicYear,
                page,
                size,
                sortBy,
                sortDir
        );

        return myClassQueryService.listMyClasses(principal.getUserId(), criteria);
    }

    @GetMapping("/classes/timeline")
    public MyClassTimelineResponse getMyClassTimeline(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return myClassTimelineService.getMyClassTimeline(principal.getUserId());
    }

    @GetMapping("/classes/teaching")
    public PageMyClassResponse listMyTeachingClasses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir
    ) {
        MyClassQueryCriteria criteria = MyClassQueryCriteria.fromRequest(
                q,
                "TEACHING",
                status,
                null,
                semester,
                academicYear,
                page,
                size,
                sortBy,
                sortDir
        );

        return myClassQueryService.listMyClasses(principal.getUserId(), criteria);
    }

    @GetMapping("/classes/semesters")
    public List<MyClassSemesterOptionResponse> listMyClassSemesters(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return myClassQueryService.listMyClassSemesters(principal.getUserId());
    }
}
