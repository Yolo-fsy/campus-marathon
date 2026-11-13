package com.campus.marathon.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 马拉松报名实体（参赛者）。
 */
public class MarathonRunner {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String PAY_UNPAID = "UNPAID";
    public static final String PAY_PAID = "PAID";
    public static final String PAY_REFUNDED = "REFUNDED";

    /** 审核通过后的缴费时限（小时）：超时未支付自动取消报名并释放名额。 */
    public static final int PAYMENT_DEADLINE_HOURS = 24;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String name;
    private String studentId;
    private String phone;
    private String college;
    private String eventCategory;
    private String status = STATUS_PENDING;
    private String remark;
    private String payStatus = PAY_UNPAID;
    private String outTradeNo;
    private java.math.BigDecimal payAmount = new java.math.BigDecimal("30.00");
    private LocalDateTime payTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MarathonRunner() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public java.math.BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(java.math.BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    /** 支付状态中文标签。 */
    public String getPayStatusLabel() {
        if (PAY_PAID.equals(payStatus)) {
            return "已支付";
        }
        if (PAY_REFUNDED.equals(payStatus)) {
            return "已退款";
        }
        return "待支付";
    }

    public String getPayTimeText() {
        return payTime == null ? "" : payTime.format(DATE_TIME_FORMATTER);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /* ---------- 供 JSP 直接展示的派生属性 ---------- */

    public String getEventCategoryLabel() {
        EventCategory category = EventCategory.fromCode(eventCategory);
        return category == null ? "未选择项目" : category.getLabel();
    }

    public String getEventCategoryDistance() {
        EventCategory category = EventCategory.fromCode(eventCategory);
        return category == null ? "" : category.getDistance();
    }

    public String getStatusLabel() {
        if (STATUS_CONFIRMED.equals(status)) {
            return "报名成功";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "驳回";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "已取消";
        }
        return "待审核";
    }

    /**
     * 状态对应的 CSS 类后缀，方便页面渲染不同颜色的标签。
     */
    public String getStatusClass() {
        if (STATUS_CONFIRMED.equals(status)) {
            return "is-success";
        }
        if (STATUS_REJECTED.equals(status)) {
            return "is-danger";
        }
        if (STATUS_CANCELLED.equals(status)) {
            return "is-muted";
        }
        return "is-warning";
    }

    public String getInitial() {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        return name.substring(0, 1);
    }

    public String getBibNumber() {
        if (id == null) {
            return "----";
        }
        return String.format("%04d", id);
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(DATE_TIME_FORMATTER);
    }

    public String getUpdatedAtText() {
        return updatedAt == null ? "" : updatedAt.format(DATE_TIME_FORMATTER);
    }
}
