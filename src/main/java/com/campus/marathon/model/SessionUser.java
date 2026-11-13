package com.campus.marathon.model;

import java.io.Serializable;

/**
 * 登录用户，存放于 Session。
 */
public class SessionUser implements Serializable {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_RUNNER = "RUNNER";
    public static final String ROLE_JUDGE = "JUDGE";

    private final String role;
    private final String username;
    private final String studentId;
    private final String displayName;
    /** 评审关联的竞赛 id（ADMIN/RUNNER 为 null） */
    private final Long contestId;

    private SessionUser(String role, String username, String studentId, String displayName, Long contestId) {
        this.role = role;
        this.username = username;
        this.studentId = studentId;
        this.displayName = displayName;
        this.contestId = contestId;
    }

    public static SessionUser admin(String username) {
        return new SessionUser(ROLE_ADMIN, username, null, "赛事管理员", null);
    }

    public static SessionUser runner(MarathonRunner runner) {
        return new SessionUser(ROLE_RUNNER, runner.getStudentId(),
                runner.getStudentId(), runner.getName(), null);
    }

    /**
     * 已注册账号但还没有报名记录时使用。
     */
    public static SessionUser runner(String studentId, String displayName) {
        return new SessionUser(ROLE_RUNNER, studentId, studentId,
                displayName == null ? "" : displayName, null);
    }

    /**
     * 评审登录时使用，携带其所属竞赛 id。
     */
    public static SessionUser judge(String username, String displayName, long contestId) {
        return new SessionUser(ROLE_JUDGE, username, null,
                displayName == null ? username : displayName, contestId);
    }

    public String getRole() {
        return role;
    }

    public String getUsername() {
        return username;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getContestId() {
        return contestId;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isRunner() {
        return ROLE_RUNNER.equals(role);
    }

    public boolean isJudge() {
        return ROLE_JUDGE.equals(role);
    }

    public String getRoleLabel() {
        if (isAdmin()) {
            return "管理员";
        }
        if (isJudge()) {
            return "评审";
        }
        return "参赛者";
    }

    public String getInitial() {
        if (displayName == null || displayName.isEmpty()) {
            return "?";
        }
        return displayName.substring(0, 1);
    }
}
