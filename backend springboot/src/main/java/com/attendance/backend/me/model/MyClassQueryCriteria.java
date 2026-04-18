package com.attendance.backend.me.model;

import com.attendance.backend.common.exception.ApiException;

import java.util.Locale;
import java.util.Set;

public class MyClassQueryCriteria {

    private static final Set<String> ALLOWED_GROUP_STATUS = Set.of("ACTIVE", "ARCHIVED");
    private static final Set<String> ALLOWED_MEMBER_STATUS = Set.of("PENDING", "APPROVED", "REJECTED", "REMOVED");

    private final String q;
    private final Scope scope;
    private final String status;
    private final String memberStatus;
    private final String semester;
    private final String academicYear;
    private final int page;
    private final int size;
    private final SortBy sortBy;
    private final SortDir sortDir;

    public MyClassQueryCriteria(
            String q,
            Scope scope,
            String status,
            String memberStatus,
            String semester,
            String academicYear,
            int page,
            int size,
            SortBy sortBy,
            SortDir sortDir
    ) {
        this.q = q;
        this.scope = scope;
        this.status = status;
        this.memberStatus = memberStatus;
        this.semester = semester;
        this.academicYear = academicYear;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDir = sortDir;
    }

    public static MyClassQueryCriteria fromRequest(
            String q,
            String scope,
            String status,
            String memberStatus,
            String semester,
            String academicYear,
            Integer page,
            Integer size,
            String sortBy,
            String sortDir
    ) {
        return new MyClassQueryCriteria(
                normalizeNullableText(q),
                parseScope(scope),
                parseEnumFilter(status, ALLOWED_GROUP_STATUS, "status"),
                parseEnumFilter(memberStatus, ALLOWED_MEMBER_STATUS, "memberStatus"),
                normalizeNullableText(semester),
                normalizeNullableText(academicYear),
                normalizePage(page),
                normalizeSize(size),
                parseSortBy(sortBy),
                parseSortDir(sortDir)
        );
    }

    public String getQ() {
        return q;
    }

    public Scope getScope() {
        return scope;
    }

    public String getStatus() {
        return status;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public String getSemester() {
        return semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public SortDir getSortDir() {
        return sortDir;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw ApiException.badRequest("INVALID_PAGE", "page must be >= 0");
        }
        return page;
    }

    private static int normalizeSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 200) {
            throw ApiException.badRequest("INVALID_SIZE", "size must be between 1 and 200");
        }
        return size;
    }

    private static Scope parseScope(String scope) {
        String normalized = normalizeNullableText(scope);
        if (normalized == null) {
            return Scope.ALL;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "ALL" -> Scope.ALL;
            case "TEACHING" -> Scope.TEACHING;
            case "STUDYING" -> Scope.STUDYING;
            default -> throw ApiException.badRequest(
                    "INVALID_SCOPE",
                    "scope must be one of [ALL, TEACHING, STUDYING]"
            );
        };
    }

    private static SortBy parseSortBy(String sortBy) {
        String normalized = normalizeNullableText(sortBy);
        if (normalized == null) {
            return SortBy.UPDATED_AT;
        }

        return switch (normalized) {
            case "updatedAt" -> SortBy.UPDATED_AT;
            case "createdAt" -> SortBy.CREATED_AT;
            case "name" -> SortBy.NAME;
            default -> throw ApiException.badRequest(
                    "INVALID_SORT_BY",
                    "sortBy must be one of [updatedAt, createdAt, name]"
            );
        };
    }

    private static SortDir parseSortDir(String sortDir) {
        String normalized = normalizeNullableText(sortDir);
        if (normalized == null) {
            return SortDir.DESC;
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        return switch (lower) {
            case "asc" -> SortDir.ASC;
            case "desc" -> SortDir.DESC;
            default -> throw ApiException.badRequest(
                    "INVALID_SORT_DIR",
                    "sortDir must be one of [asc, desc]"
            );
        };
    }

    private static String parseEnumFilter(String value, Set<String> allowed, String fieldName) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            return null;
        }

        String upper = normalized.toUpperCase(Locale.ROOT);
        if (!allowed.contains(upper)) {
            throw ApiException.badRequest(
                    "INVALID_" + fieldName.toUpperCase(Locale.ROOT),
                    fieldName + " has invalid value"
            );
        }

        return upper;
    }

    public enum Scope {
        ALL,
        TEACHING,
        STUDYING
    }

    public enum SortBy {
        UPDATED_AT,
        CREATED_AT,
        NAME
    }

    public enum SortDir {
        ASC,
        DESC
    }
}