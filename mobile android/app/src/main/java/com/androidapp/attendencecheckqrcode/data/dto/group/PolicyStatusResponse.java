package com.androidapp.attendencecheckqrcode.data.dto.group;

import java.io.Serializable;
import java.util.List;

public class PolicyStatusResponse implements Serializable {

    private String groupId;
    private String groupName;
    private Policy policy;
    private int closedSessionCount;
    private int eligibleSessionCount;
    private int presentCount;
    private int lateCount;
    private int absentCount;
    private int excusedCount;
    private double earnedAttendancePoints;
    private double attendanceRate;
    private String policyStatus;
    private List<String> breachReasons;

    // --- GETTERS & SETTERS DÀNH CHO POLICY STATUS ---

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Policy getPolicy() { return policy; }
    public void setPolicy(Policy policy) { this.policy = policy; }

    public int getClosedSessionCount() { return closedSessionCount; }
    public void setClosedSessionCount(int closedSessionCount) { this.closedSessionCount = closedSessionCount; }

    public int getEligibleSessionCount() { return eligibleSessionCount; }
    public void setEligibleSessionCount(int eligibleSessionCount) { this.eligibleSessionCount = eligibleSessionCount; }

    public int getPresentCount() { return presentCount; }
    public void setPresentCount(int presentCount) { this.presentCount = presentCount; }

    public int getLateCount() { return lateCount; }
    public void setLateCount(int lateCount) { this.lateCount = lateCount; }

    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }

    public int getExcusedCount() { return excusedCount; }
    public void setExcusedCount(int excusedCount) { this.excusedCount = excusedCount; }

    public double getEarnedAttendancePoints() { return earnedAttendancePoints; }
    public void setEarnedAttendancePoints(double earnedAttendancePoints) { this.earnedAttendancePoints = earnedAttendancePoints; }

    public double getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(double attendanceRate) { this.attendanceRate = attendanceRate; }

    public String getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(String policyStatus) { this.policyStatus = policyStatus; }

    public List<String> getBreachReasons() { return breachReasons; }
    public void setBreachReasons(List<String> breachReasons) { this.breachReasons = breachReasons; }

    // ========================================================
    // LỚP POLICY (NỘI QUY) NẰM LỒNG BÊN TRONG
    // ========================================================
    public static class Policy implements Serializable {
        private String groupId;
        private String source;
        private double lateWeight;
        private double warningBelowRate;
        private double criticalBelowRate;
        private int warningAbsentCount;
        private int criticalAbsentCount; // Quan trọng nhất: Số buổi vắng tối đa cấm thi
        private String excusedHandling;
        private String sessionScope;
        private String membershipScope;
        private String createdAt;
        private String createdByUserId;
        private String updatedAt;
        private String updatedByUserId;

        // --- GETTERS & SETTERS DÀNH CHO POLICY ---

        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

        public double getLateWeight() { return lateWeight; }
        public void setLateWeight(double lateWeight) { this.lateWeight = lateWeight; }

        public double getWarningBelowRate() { return warningBelowRate; }
        public void setWarningBelowRate(double warningBelowRate) { this.warningBelowRate = warningBelowRate; }

        public double getCriticalBelowRate() { return criticalBelowRate; }
        public void setCriticalBelowRate(double criticalBelowRate) { this.criticalBelowRate = criticalBelowRate; }

        public int getWarningAbsentCount() { return warningAbsentCount; }
        public void setWarningAbsentCount(int warningAbsentCount) { this.warningAbsentCount = warningAbsentCount; }

        public int getCriticalAbsentCount() { return criticalAbsentCount; }
        public void setCriticalAbsentCount(int criticalAbsentCount) { this.criticalAbsentCount = criticalAbsentCount; }

        public String getExcusedHandling() { return excusedHandling; }
        public void setExcusedHandling(String excusedHandling) { this.excusedHandling = excusedHandling; }

        public String getSessionScope() { return sessionScope; }
        public void setSessionScope(String sessionScope) { this.sessionScope = sessionScope; }

        public String getMembershipScope() { return membershipScope; }
        public void setMembershipScope(String membershipScope) { this.membershipScope = membershipScope; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getCreatedByUserId() { return createdByUserId; }
        public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

        public String getUpdatedByUserId() { return updatedByUserId; }
        public void setUpdatedByUserId(String updatedByUserId) { this.updatedByUserId = updatedByUserId; }
    }
}