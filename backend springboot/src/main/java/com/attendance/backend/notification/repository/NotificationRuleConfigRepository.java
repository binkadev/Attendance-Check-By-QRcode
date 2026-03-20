package com.attendance.backend.notification.repository;

import com.attendance.backend.domain.entity.NotificationRuleConfig;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRuleConfigRepository extends JpaRepository<NotificationRuleConfig, UUID> {

    @Query(value = """
        select *
        from notification_rule_configs c
        where c.type = :type
          and c.channel = :channel
          and (c.group_id = :groupId or c.group_id is null)
        order by case when c.group_id = :groupId then 0 else 1 end
        limit 1
    """, nativeQuery = true)
    NotificationRuleConfig findEffectiveRule(
            @Param("groupId") UUID groupId,
            @Param("type") String type,
            @Param("channel") String channel
    );
}