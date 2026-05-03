package com.attendance.backend.me.service.impl;

import com.attendance.backend.me.dto.MyClassTimelineItemResponse;
import com.attendance.backend.me.dto.MyClassTimelineResponse;
import com.attendance.backend.me.repository.MyClassTimelineRepository;
import com.attendance.backend.me.service.MyClassTimelineService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MyClassTimelineServiceImpl implements MyClassTimelineService {

    private final MyClassTimelineRepository myClassTimelineRepository;

    public MyClassTimelineServiceImpl(MyClassTimelineRepository myClassTimelineRepository) {
        this.myClassTimelineRepository = myClassTimelineRepository;
    }

    @Override
    public MyClassTimelineResponse getMyClassTimeline(UUID actorUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate tomorrow = today.plusDays(1);

        List<MyClassTimelineItemResponse> items = myClassTimelineRepository.findMyClassTimeline(
                actorUserId,
                today,
                tomorrow,
                now
        );

        MyClassTimelineResponse response = new MyClassTimelineResponse();
        response.setToday(today);
        response.setTomorrow(tomorrow);
        response.setServerTime(now);
        response.setOngoing(filterByBucket(items, "ONGOING"));
        response.setUpcomingToday(filterByBucket(items, "UPCOMING_TODAY"));
        response.setUpcomingTomorrow(filterByBucket(items, "UPCOMING_TOMORROW"));
        return response;
    }

    private List<MyClassTimelineItemResponse> filterByBucket(List<MyClassTimelineItemResponse> items, String bucket) {
        return items.stream()
                .filter(item -> bucket.equals(item.getBucket()))
                .toList();
    }
}
