package com.attendance.backend.me.service;

import com.attendance.backend.me.api.response.MyClassTimelineResponse;

import java.util.UUID;

public interface MyClassTimelineService {

    MyClassTimelineResponse getMyClassTimeline(UUID actorUserId);
}
