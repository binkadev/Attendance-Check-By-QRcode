package com.attendance.backend.attendance.repository;

import com.attendance.backend.domain.entity.AttendancePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface AttendancePolicyRepository extends JpaRepository<AttendancePolicy, UUID> {

    Optional<AttendancePolicy> findByGroupId(UUID groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AttendancePolicy p where p.groupId = :groupId")
    Optional<AttendancePolicy> findByGroupIdForUpdate(@Param("groupId") UUID groupId);
}