package com.attendance.backend.me.service;

import com.attendance.backend.me.dto.MyClassSemesterOptionResponse;
import com.attendance.backend.me.dto.PageMyClassResponse;
import com.attendance.backend.me.model.MyClassQueryCriteria;

import java.util.List;
import java.util.UUID;

public interface MyClassQueryService {

    PageMyClassResponse listMyClasses(UUID actorUserId, MyClassQueryCriteria criteria);

    List<MyClassSemesterOptionResponse> listMyClassSemesters(UUID actorUserId);
}