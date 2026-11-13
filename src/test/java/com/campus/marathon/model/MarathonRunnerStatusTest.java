package com.campus.marathon.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 报名状态标签与样式映射的单元测试（覆盖新增的 CANCELLED 状态）。
 */
class MarathonRunnerStatusTest {

    @Test
    @DisplayName("CANCELLED 状态显示「已取消」并使用 is-muted 样式")
    void cancelledStatusLabelAndClass() {
        MarathonRunner runner = new MarathonRunner();
        runner.setStatus(MarathonRunner.STATUS_CANCELLED);

        assertEquals("已取消", runner.getStatusLabel());
        assertEquals("is-muted", runner.getStatusClass());
    }

    @Test
    @DisplayName("原有状态映射不被 CANCELLED 影响")
    void existingStatusMappingUnchanged() {
        MarathonRunner runner = new MarathonRunner();

        runner.setStatus(MarathonRunner.STATUS_PENDING);
        assertEquals("待审核", runner.getStatusLabel());
        assertEquals("is-warning", runner.getStatusClass());

        runner.setStatus(MarathonRunner.STATUS_CONFIRMED);
        assertEquals("报名成功", runner.getStatusLabel());
        assertEquals("is-success", runner.getStatusClass());

        runner.setStatus(MarathonRunner.STATUS_REJECTED);
        assertEquals("驳回", runner.getStatusLabel());
        assertEquals("is-danger", runner.getStatusClass());
    }

    @Test
    @DisplayName("默认状态为待审核")
    void defaultStatusIsPending() {
        MarathonRunner runner = new MarathonRunner();

        assertEquals(MarathonRunner.STATUS_PENDING, runner.getStatus());
        assertEquals("待审核", runner.getStatusLabel());
    }
}
