package com.attendance.backend.group.repository;

import com.attendance.backend.domain.enums.MemberRole;
import com.attendance.backend.domain.enums.MemberStatus;
import com.attendance.backend.domain.entity.GroupMember;
import com.attendance.backend.domain.id.GroupMemberId;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    @Query("""
        select gm
        from GroupMember gm
        where gm.id.groupId = :groupId
          and gm.id.userId = :userId
    """)
    Optional<GroupMember> findByGroupIdAndUserId(@Param("groupId") UUID groupId,
                                                 @Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select gm
        from GroupMember gm
        where gm.id.groupId = :groupId
          and gm.id.userId = :userId
    """)
    Optional<GroupMember> findByGroupIdAndUserIdForUpdate(@Param("groupId") UUID groupId,
                                                          @Param("userId") UUID userId);

    @Query("""
        select gm
        from GroupMember gm
        where gm.id.groupId = :groupId
        order by
          case gm.role
            when com.attendance.backend.domain.enums.MemberRole.OWNER then 0
            when com.attendance.backend.domain.enums.MemberRole.CO_HOST then 1
            else 2
          end,
          gm.createdAt desc
    """)
    Page<GroupMember> findPageByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

    @Query("""
        select (count(gm) > 0)
        from GroupMember gm
        where gm.id.groupId = :groupId
          and gm.id.userId = :userId
          and gm.memberStatus = com.attendance.backend.domain.enums.MemberStatus.APPROVED
    """)
    boolean existsApprovedByGroupIdAndUserId(@Param("groupId") UUID groupId,
                                             @Param("userId") UUID userId);

    @Query("""
        select gm
        from GroupMember gm
        where gm.id.groupId = :groupId
          and gm.role = com.attendance.backend.domain.enums.MemberRole.OWNER
    """)
    Optional<GroupMember> findOwnerByGroupId(@Param("groupId") UUID groupId);

    @Query("""
            select count(gm)
            from GroupMember gm
            where gm.id.groupId = :groupId
              and gm.role = :role
              and gm.memberStatus = :memberStatus
              and gm.removedAt is null
            """)
    long countByGroupIdAndRoleAndMemberStatus(
            @Param("groupId") UUID groupId,
            @Param("role") MemberRole role,
            @Param("memberStatus") MemberStatus memberStatus
    );

    @Query("""
            select u.fullName
            from User u
            where u.id = :userId
              and u.deletedAt is null
            """)
    Optional<String> findUserFullNameById(@Param("userId") UUID userId);
}