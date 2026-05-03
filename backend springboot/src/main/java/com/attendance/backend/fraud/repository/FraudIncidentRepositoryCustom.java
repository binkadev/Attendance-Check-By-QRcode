package com.attendance.backend.fraud.repository;

import com.attendance.backend.fraud.dto.FraudIncidentFilter;
import com.attendance.backend.fraud.entity.FraudIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FraudIncidentRepositoryCustom {

    Page<FraudIncident> search(UUID groupId, FraudIncidentFilter filter, Pageable pageable);
}
