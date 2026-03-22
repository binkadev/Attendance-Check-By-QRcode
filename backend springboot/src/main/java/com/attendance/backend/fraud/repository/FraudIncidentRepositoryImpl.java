package com.attendance.backend.fraud.repository;

import com.attendance.backend.fraud.dto.FraudIncidentFilter;
import com.attendance.backend.fraud.dto.FraudIncidentSortBy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class FraudIncidentRepositoryImpl implements FraudIncidentRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<FraudIncident> search(UUID groupId, FraudIncidentFilter filter, Pageable pageable) {
        StringBuilder from = new StringBuilder(
                " from fraud_incidents fi where fi.group_id = UUID_TO_BIN(:groupId, 1) "
        );

        List<String> conditions = new ArrayList<>();

        if (filter != null) {
            if (filter.assignedToUserId() != null) {
                conditions.add(" fi.assigned_to_user_id = UUID_TO_BIN(:assignedToUserId, 1) ");
            }
            if (filter.statuses() != null && !filter.statuses().isEmpty()) {
                conditions.add(" fi.status in (:statuses) ");
            }
            if (filter.types() != null && !filter.types().isEmpty()) {
                conditions.add(" fi.type in (:types) ");
            }
            if (filter.severities() != null && !filter.severities().isEmpty()) {
                conditions.add(" fi.severity in (:severities) ");
            }
        }

        if (!conditions.isEmpty()) {
            from.append(" and ").append(String.join(" and ", conditions));
        }

        FraudIncidentSortBy sortBy = filter == null
                ? FraudIncidentSortBy.LAST_DETECTED_AT
                : filter.normalizedSortBy();

        String orderBy = switch (sortBy) {
            case CREATED_AT -> " fi.created_at ";
            case SEVERITY -> " fi.severity ";
            case LAST_DETECTED_AT -> " fi.last_detected_at ";
        };

        String direction = (filter != null && filter.normalizedSortDir().isAscending())
                ? " asc "
                : " desc ";

        Query dataQuery = entityManager.createNativeQuery(
                "select fi.* " + from + " order by " + orderBy + direction + ", fi.id desc ",
                FraudIncident.class
        );
        Query countQuery = entityManager.createNativeQuery("select count(*) " + from);

        bindParams(dataQuery, groupId, filter);
        bindParams(countQuery, groupId, filter);

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<FraudIncident> items = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(items, pageable, total);
    }

    private void bindParams(Query query, UUID groupId, FraudIncidentFilter filter) {
        query.setParameter("groupId", uuidParam(groupId));

        if (filter == null) {
            return;
        }

        if (filter.assignedToUserId() != null) {
            query.setParameter("assignedToUserId", uuidParam(filter.assignedToUserId()));
        }
        if (filter.statuses() != null && !filter.statuses().isEmpty()) {
            query.setParameter("statuses", filter.statuses().stream().map(Enum::name).toList());
        }
        if (filter.types() != null && !filter.types().isEmpty()) {
            query.setParameter("types", filter.types().stream().map(Enum::name).toList());
        }
        if (filter.severities() != null && !filter.severities().isEmpty()) {
            query.setParameter("severities", filter.severities().stream().map(Enum::name).toList());
        }
    }

    private String uuidParam(UUID value) {
        return value == null ? null : value.toString();
    }
}