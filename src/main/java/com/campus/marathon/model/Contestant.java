package com.campus.marathon.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 参赛者实体：参与某场竞赛的选手或团队。
 */
public class Contestant implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long contestId;
    private String name;
    private String studentId;
    private String teamName;
    private LocalDateTime submittedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getContestId() {
        return contestId;
    }

    public void setContestId(long contestId) {
        this.contestId = contestId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /** 展示名：团队优先，否则用姓名 */
    public String getDisplayName() {
        if (teamName != null && !teamName.trim().isEmpty()) {
            return teamName;
        }
        return name;
    }
}
