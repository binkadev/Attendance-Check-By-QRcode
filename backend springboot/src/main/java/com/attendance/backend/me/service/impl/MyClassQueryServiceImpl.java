package com.attendance.backend.me.service.impl;

import com.attendance.backend.me.dto.MyClassSemesterOptionResponse;
import com.attendance.backend.me.dto.PageMyClassResponse;
import com.attendance.backend.me.model.MyClassQueryCriteria;
import com.attendance.backend.me.repository.MyClassQueryRepository;
import com.attendance.backend.me.service.MyClassQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MyClassQueryServiceImpl implements MyClassQueryService {

    private final MyClassQueryRepository myClassQueryRepository;

    public MyClassQueryServiceImpl(MyClassQueryRepository myClassQueryRepository) {
        this.myClassQueryRepository = myClassQueryRepository;
    }

    @Override
    public PageMyClassResponse listMyClasses(UUID actorUserId, MyClassQueryCriteria criteria) {
        return myClassQueryRepository.findMyClasses(actorUserId, criteria);
    }

    @Override
    public List<MyClassSemesterOptionResponse> listMyClassSemesters(UUID actorUserId) {
        return myClassQueryRepository.findMyClassSemesters(actorUserId);
    }
}