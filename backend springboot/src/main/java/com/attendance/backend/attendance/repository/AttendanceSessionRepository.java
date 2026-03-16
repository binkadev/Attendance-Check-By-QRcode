package com.attendance.backend.attendance.repository;

import com.attendance.backend.domain.entity.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    @Query(value = """
        select *
        from attendance_sessions
        where id = UUID_TO_BIN(:id, 1)
          and deleted_at is null
        for share
        """, nativeQuery = true)
    Optional<AttendanceSession> findByIdForShare(@Param("id") String id);

    @Query(value = """
        select *
        from attendance_sessions
        where id = UUID_TO_BIN(:id, 1)
          and deleted_at is null
        for update
        """, nativeQuery = true)
    Optional<AttendanceSession> findByIdForUpdate(@Param("id") String id);

    default Optional<AttendanceSession> findByIdForShare(UUID id) {
        return findByIdForShare(id.toString());
    }

    default Optional<AttendanceSession> findByIdForUpdate(UUID id) {
        return findByIdForUpdate(id.toString());
    }
}