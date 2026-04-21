package com.attendance.backend.group.repository;

import com.attendance.backend.domain.entity.ClassGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, UUID> {

    @Query("""
        select g
        from ClassGroup g
        where g.joinCode = :joinCode
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findByJoinCodeActive(@Param("joinCode") String joinCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select g
        from ClassGroup g
        where g.joinCode = :joinCode
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findByJoinCodeActiveForUpdate(@Param("joinCode") String joinCode);

    @Query("""
        select g
        from ClassGroup g
        where g.id = :groupId
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findActiveById(@Param("groupId") UUID groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select g
        from ClassGroup g
        where g.id = :groupId
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findActiveByIdForUpdate(@Param("groupId") UUID groupId);

    @Query("""
        select g
        from ClassGroup g
        where g.id = :groupId
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findByIdAndDeletedAtIsNull(@Param("groupId") UUID groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select g
        from ClassGroup g
        where g.id = :groupId
          and g.deletedAt is null
    """)
    Optional<ClassGroup> findByIdAndDeletedAtIsNullForUpdate(@Param("groupId") UUID groupId);
}
