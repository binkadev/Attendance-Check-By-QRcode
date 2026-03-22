package com.attendance.backend.fraud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FraudIncidentRepository extends JpaRepository<FraudIncident, UUID>, FraudIncidentRepositoryCustom {

    Optional<FraudIncident> findByDedupKey(String dedupKey);

    Optional<FraudIncident> findByIdAndGroupId(UUID id, UUID groupId);
}
