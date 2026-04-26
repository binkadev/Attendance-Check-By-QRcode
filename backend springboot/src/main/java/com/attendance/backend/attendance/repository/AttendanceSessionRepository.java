package com.attendance.backend.attendance.repository;

import com.attendance.backend.domain.entity.AttendanceSession;
import com.attendance.backend.domain.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    @Query("""
            select s
            from AttendanceSession s
            where s.id = :sessionId
              and s.deletedAt is null
            """)
    Optional<AttendanceSession> findActiveById(@Param("sessionId") UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from AttendanceSession s
            where s.id = :sessionId
              and s.deletedAt is null
            """)
    Optional<AttendanceSession> findActiveByIdForUpdate(@Param("sessionId") UUID sessionId);

    Optional<AttendanceSession> findFirstByGroupIdAndStatusAndDeletedAtIsNullOrderByStartAtDesc(
            UUID groupId,
            SessionStatus status
    );

    boolean existsByGroupIdAndStatusAndDeletedAtIsNull(
            UUID groupId,
            SessionStatus status
    );

    default Optional<AttendanceSession> findOpenByGroupId(UUID groupId) {
        return findFirstByGroupIdAndStatusAndDeletedAtIsNullOrderByStartAtDesc(groupId, SessionStatus.OPEN);
    }

    default boolean existsOpenByGroupId(UUID groupId) {
        return existsByGroupIdAndStatusAndDeletedAtIsNull(groupId, SessionStatus.OPEN);
    }

    @Query("""
            select s
            from AttendanceSession s
            where s.groupId = :groupId
              and s.deletedAt is null
              and (:fromDate is null or s.sessionDate >= :fromDate)
              and (:toDate is null or s.sessionDate <= :toDate)
              and (:status is null or s.status = :status)
            order by s.sessionDate desc, s.startAt desc, s.createdAt desc
            """)
    Page<AttendanceSession> findPageByGroupId(
            @Param("groupId") UUID groupId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") SessionStatus status,
            Pageable pageable
    );
}