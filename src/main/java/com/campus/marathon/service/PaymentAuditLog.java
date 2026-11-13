package com.campus.marathon.service;

import com.campus.marathon.model.MarathonRunner;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 支付订单审计日志。
 * 以固定 key=value 单行格式记录订单生命周期（下单 / 到账 / 退款 / 取消），
 * 便于按订单号检索、与支付宝账单逐笔对账。
 * 升级为专用单调递增的日志器，不影响业务异常处理。
 */
final class PaymentAuditLog {

    private static final Logger LOGGER = Logger.getLogger("com.campus.marathon.audit");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private PaymentAuditLog() {
    }

    /** 下单：生成商户订单号，等待支付。 */
    static void orderCreated(MarathonRunner runner) {
        write(Level.INFO, "ORDER_CREATE", runner, null);
    }

    /** 到账：支付成功且名额已确认。 */
    static void paid(MarathonRunner runner) {
        write(Level.INFO, "ORDER_PAID", runner, null);
    }

    /** 重复回调：幂等命中，不重复入账。 */
    static void duplicateCallback(MarathonRunner runner) {
        write(Level.INFO, "CALLBACK_DUPLICATE", runner, null);
    }

    /** 订单号不存在：疑似伪造回调或订单已被清理。 */
    static void unknownOrder(String outTradeNo) {
        LOGGER.warning("AUDIT event=CALLBACK_UNKNOWN outTradeNo=" + safe(outTradeNo)
                + " time=" + LocalDateTime.now().format(TIME_FORMAT));
    }

    /** 退款成功。source 标识退款来源：userCancel（用户取消）/ capacityFull（名额已满自动退款）。 */
    static void refunded(MarathonRunner runner, String source) {
        write(Level.INFO, "ORDER_REFUND", runner, "source=" + source);
    }

    /** 退款失败：订单保持原状，需人工介入。 */
    static void refundFailed(MarathonRunner runner) {
        write(Level.WARNING, "REFUND_FAILED", runner, null);
    }

    /** 未支付订单直接取消：无需退款。 */
    static void cancelled(MarathonRunner runner) {
        write(Level.INFO, "ORDER_CANCEL", runner, null);
    }

    private static void write(Level level, String event, MarathonRunner runner, String extra) {
        if (runner == null) {
            LOGGER.log(level, "AUDIT event=" + event + " runner=null");
            return;
        }
        StringBuilder line = new StringBuilder("AUDIT event=").append(event)
                .append(" outTradeNo=").append(safe(runner.getOutTradeNo()))
                .append(" runnerId=").append(runner.getId())
                .append(" studentId=").append(safe(runner.getStudentId()))
                .append(" amount=").append(runner.getPayAmount() == null
                        ? "-" : runner.getPayAmount().toPlainString())
                .append(" payStatus=").append(safe(runner.getPayStatus()))
                .append(" status=").append(safe(runner.getStatus()))
                .append(" time=").append(LocalDateTime.now().format(TIME_FORMAT));
        if (extra != null) {
            line.append(' ').append(extra);
        }
        LOGGER.log(level, line.toString());
    }

    private static String safe(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }
}
