package com.attendance.backend.attendance.repository;

import com.attendance.backend.domain.entity.AttendancePolicy;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttendancePolicyQueryRepository extends Repository<AttendancePolicy, UUID> {

    @Query(value = """
            select
                BIN_TO_UUID(g.id, 1) as groupId,
                g.name as groupName,
                g.status as groupStatus
            from class_groups g
            where g.id = UUID_TO_BIN(:groupId, 1)
              and g.deleted_at is null
            limit 1
            """, nativeQuery = true)
    AttendancePolicyGroupBasicProjection findGroupBasic(
            @Param("groupId") String groupId
    );

    @Query(value = """
            select
                gm.role as role,
                gm.member_status as memberStatus
            from group_members gm
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.user_id = UUID_TO_BIN(:userId, 1)
            limit 1
            """, nativeQuery = true)
    AttendancePolicyMembershipAccessProjection findMembershipAccess(
            @Param("groupId") String groupId,
            @Param("userId") String userId
    );

    @Query(value = """
            select
                BIN_TO_UUID(gm.user_id, 1) as userId,
                u.full_name as fullName,
                u.email as email,
                coalesce(gm.joined_at, gm.created_at) as joinedAt,
                count(s.id) as closedSessionCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'PRESENT' then 1 else 0 end) as presentCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'LATE' then 1 else 0 end) as lateCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'ABSENT' then 1 else 0 end) as absentCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'EXCUSED' then 1 else 0 end) as excusedCount
            from group_members gm
            join users u
              on u.id = gm.user_id
            left join attendance_sessions s
              on s.group_id = gm.group_id
             and s.status = 'CLOSED'
             and s.deleted_at is null
             and s.start_at >= coalesce(gm.joined_at, gm.created_at)
            left join session_attendance sa
              on sa.session_id = s.id
             and sa.user_id = gm.user_id
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.user_id = UUID_TO_BIN(:userId, 1)
              and gm.member_status = 'APPROVED'
            group by gm.user_id, u.full_name, u.email, coalesce(gm.joined_at, gm.created_at)
            """, nativeQuery = true)
    AttendancePolicyStudentAggregateProjection aggregateForApprovedMember(
            @Param("groupId") String groupId,
            @Param("userId") String userId
    );

    @Query(value = """
            select
                m.userId as userId,
                m.fullName as fullName,
                m.email as email,
                m.joinedAt as joinedAt,
                count(s.id) as closedSessionCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'PRESENT' then 1 else 0 end) as presentCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'LATE' then 1 else 0 end) as lateCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'ABSENT' then 1 else 0 end) as absentCount,
                sum(case when s.id is not null and coalesce(sa.attendance_status, 'ABSENT') = 'EXCUSED' then 1 else 0 end) as excusedCount
            from (
                select
                    BIN_TO_UUID(gm.user_id, 1) as userId,
                    u.full_name as fullName,
                    u.email as email,
                    coalesce(gm.joined_at, gm.created_at) as joinedAt
                from group_members gm
                join users u
                  on u.id = gm.user_id
                where gm.group_id = UUID_TO_BIN(:groupId, 1)
                  and gm.member_status = 'APPROVED'
                  and (
                        :q is null
                        or trim(:q) = ''
                        or lower(u.full_name) like lower(concat('%', :q, '%'))
                        or lower(u.email) like lower(concat('%', :q, '%'))
                      )
                order by
                    case when :sortBy = 'email' and :sortDir = 'asc' then u.email end asc,
                    case when :sortBy = 'email' and :sortDir = 'desc' then u.email end desc,
                    case when :sortBy = 'joinedAt' and :sortDir = 'asc' then coalesce(gm.joined_at, gm.created_at) end asc,
                    case when :sortBy = 'joinedAt' and :sortDir = 'desc' then coalesce(gm.joined_at, gm.created_at) end desc,
                    case when :sortBy = 'fullName' and :sortDir = 'asc' then u.full_name end asc,
                    case when :sortBy = 'fullName' and :sortDir = 'desc' then u.full_name end desc,
                    u.full_name asc,
                    gm.user_id asc
                limit :limit offset :offset
            ) m
            left join attendance_sessions s
              on s.group_id = UUID_TO_BIN(:groupId, 1)
             and s.status = 'CLOSED'
             and s.deleted_at is null
             and s.start_at >= m.joinedAt
            left join session_attendance sa
              on sa.session_id = s.id
             and sa.user_id = UUID_TO_BIN(m.userId, 1)
            group by m.userId, m.fullName, m.email, m.joinedAt
            order by
                case when :sortBy = 'email' and :sortDir = 'asc' then m.email end asc,
                case when :sortBy = 'email' and :sortDir = 'desc' then m.email end desc,
                case when :sortBy = 'joinedAt' and :sortDir = 'asc' then m.joinedAt end asc,
                case when :sortBy = 'joinedAt' and :sortDir = 'desc' then m.joinedAt end desc,
                case when :sortBy = 'fullName' and :sortDir = 'asc' then m.fullName end asc,
                case when :sortBy = 'fullName' and :sortDir = 'desc' then m.fullName end desc,
                m.fullName asc,
                m.userId asc
            """, nativeQuery = true)
    List<AttendancePolicyStudentAggregateProjection> aggregatePageForApprovedMembers(
            @Param("groupId") String groupId,
            @Param("q") String q,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
            select count(*)
            from group_members gm
            join users u
              on u.id = gm.user_id
            where gm.group_id = UUID_TO_BIN(:groupId, 1)
              and gm.member_status = 'APPROVED'
              and (
                    :q is null
                    or trim(:q) = ''
                    or lower(u.full_name) like lower(concat('%', :q, '%'))
                    or lower(u.email) like lower(concat('%', :q, '%'))
                  )
            """, nativeQuery = true)
    long countApprovedMembers(
            @Param("groupId") String groupId,
            @Param("q") String q
    );
}