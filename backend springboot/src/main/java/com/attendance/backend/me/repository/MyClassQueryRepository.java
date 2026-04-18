package com.attendance.backend.me.repository;

import com.attendance.backend.me.api.response.MyClassSemesterOptionResponse;
import com.attendance.backend.me.api.response.PageMyClassResponse;
import com.attendance.backend.me.model.MyClassQueryCriteria;

import java.util.List;
import java.util.UUID;

public interface MyClassQueryRepository {

    PageMyClassResponse findMyClasses(UUID actorUserId, MyClassQueryCriteria criteria);

    List<MyClassSemesterOptionResponse> findMyClassSemesters(UUID actorUserId);
}