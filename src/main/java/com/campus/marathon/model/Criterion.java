package com.campus.marathon.model;

import java.io.Serializable;

/**
 * 评分维度实体：每场竞赛可配置多个评分维度，权重之和必须为 100。
 */
public class Criterion implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private long contestId;
    private String name;
    /** 权重百分比，0-100 */
    private int weight;
    private String description;

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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
