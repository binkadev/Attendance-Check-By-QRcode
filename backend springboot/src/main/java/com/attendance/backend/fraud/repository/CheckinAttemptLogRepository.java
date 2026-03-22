package com.attendance.backend.fraud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CheckinAttemptLogRepository extends JpaRepository<CheckinAttemptLog, UUID>, CheckinAttemptLogRepositoryCustom {
}
