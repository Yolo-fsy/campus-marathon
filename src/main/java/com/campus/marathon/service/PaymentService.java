package com.campus.marathon.service;

import com.alipay.api.AlipayApiException;
import com.campus.marathon.dao.RegistrationDAO;
import com.campus.marathon.dao.impl.RegistrationDAOImpl;
import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报名支付服务：下单、支付到账（名额控制）、取消退款。
 */
public class PaymentService {

    /** 支付到账处理结果。 */
    public enum PayResult {
        /** 支付成功，报名确认 */
        SUCCESS,
        /** 订单号不存在 */
        NOT_FOUND,
        /** 已支付过（幂等，重复回调直接视为成功） */
        ALREADY_PAID,
        /** 到账时名额已满：已自动退款并取消报名 */
        FULL_REFUNDED
    }

    /** 到账结果，携带报名记录用于会话登录。 */
    public static class PayOutcome {
        private final PayResult result;
        private final MarathonRunner runner;

        PayOutcome(PayResult result, MarathonRunner runner) {
            this.result = result;
            this.runner = runner;
        }

        public PayResult getResult() {
            return result;
        }

        public MarathonRunner getRunner() {
            return runner;
        }
    }

    private final RegistrationDAO registrationDAO;
    private final AlipayService alipayService;

    public PaymentService() {
        this(new RegistrationDAOImpl(), AlipayService.loadDefault());
    }

    public PaymentService(RegistrationDAO registrationDAO, AlipayService alipayService) {
        this.registrationDAO = registrationDAO;
        this.alipayService = alipayService;
    }

    public AlipayService getAlipayService() {
        return alipayService;
    }

    /**
     * 为报名记录生成支付订单号并预占检查名额（不超卖：下单前校验剩余名额）。
     * 返回商户订单号。
     */
    public String createOrder(MarathonRunner runner) throws ValidationException, SQLException {
        LocalDateTime deadline = LocalDateTime.now().minusHours(MarathonRunner.PAYMENT_DEADLINE_HOURS);
        if (MarathonRunner.STATUS_CONFIRMED.equals(runner.getStatus())
                && !MarathonRunner.PAY_PAID.equals(runner.getPayStatus())
                && runner.getUpdatedAt() != null && runner.getUpdatedAt().isBefore(deadline)) {
            // 审核通过后超过缴费时限仍未支付：取消报名并释放名额，拒绝继续下单
            registrationDAO.updateStatus(runner.getId(), MarathonRunner.STATUS_CANCELLED);
            runner.setStatus(MarathonRunner.STATUS_CANCELLED);
            PaymentAuditLog.cancelled(runner);
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("status", "已超过 " + MarathonRunner.PAYMENT_DEADLINE_HOURS
                    + " 小时缴费时限，报名已自动取消，请重新报名");
            throw new ValidationException(errors);
        }
        EventCategory category = EventCategory.fromCode(runner.getEventCategory());
        int used = registrationDAO.countConfirmedByCategory(category.getCode(), runner.getId());
        if (used >= category.getCapacity()) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("eventCategory", category.getLabel() + "名额已满（限额 "
                    + category.getCapacity() + " 人），暂无法缴费，请改报其他项目");
            throw new ValidationException(errors);
        }
        String outTradeNo = "MR" + runner.getId() + System.currentTimeMillis();
        registrationDAO.updatePayment(runner.getId(), outTradeNo, MarathonRunner.PAY_UNPAID, null);
        runner.setOutTradeNo(outTradeNo);
        PaymentAuditLog.orderCreated(runner);
        return outTradeNo;
    }

    /**
     * 支付到账处理（同步跳转与异步通知共用，幂等）。
     * 到账时再次校验名额；若名额已满则自动退款并取消报名，保证不超卖。
     */
    public PayOutcome handlePaid(String outTradeNo) throws SQLException {
        MarathonRunner runner = registrationDAO.findByOutTradeNo(outTradeNo);
        if (runner == null) {
            PaymentAuditLog.unknownOrder(outTradeNo);
            return new PayOutcome(PayResult.NOT_FOUND, null);
        }
        if (MarathonRunner.PAY_PAID.equals(runner.getPayStatus())) {
            PaymentAuditLog.duplicateCallback(runner);
            return new PayOutcome(PayResult.ALREADY_PAID, runner);
        }
        EventCategory category = EventCategory.fromCode(runner.getEventCategory());
        // 先落支付信息，再用原子 UPDATE 抢占名额；抢失败说明名额已被并发到账占满，自动退款并取消报名
        registrationDAO.updatePayment(runner.getId(), outTradeNo, MarathonRunner.PAY_PAID, LocalDateTime.now());
        runner.setPayStatus(MarathonRunner.PAY_PAID);
        boolean confirmed = CapacityGuard.confirmWithCapacity(
                registrationDAO, runner.getId(), category.getCode(), category.getCapacity());
        if (!confirmed) {
            refundAndCancel(runner);
            return new PayOutcome(PayResult.FULL_REFUNDED, runner);
        }
        runner.setStatus(MarathonRunner.STATUS_CONFIRMED);
        PaymentAuditLog.paid(runner);
        return new PayOutcome(PayResult.SUCCESS, runner);
    }

    /**
     * 参赛者取消报名：已支付的先退款，再将状态置为已取消；记录保留用于赛后统计。
     * 返回取消结果文案编码。
     */
    public String cancelWithRefund(String studentId) throws SQLException {
        MarathonRunner runner = registrationDAO.findByStudentId(studentId);
        if (runner == null || MarathonRunner.STATUS_CANCELLED.equals(runner.getStatus())) {
            return "cancelFailed";
        }
        boolean wasPaid = MarathonRunner.PAY_PAID.equals(runner.getPayStatus());
        if (wasPaid) {
            boolean refunded = refundQuietly(runner);
            if (!refunded) {
                PaymentAuditLog.refundFailed(runner);
                return "refundFailed";
            }
            registrationDAO.updatePayment(runner.getId(), runner.getOutTradeNo(),
                    MarathonRunner.PAY_REFUNDED, runner.getPayTime());
            runner.setPayStatus(MarathonRunner.PAY_REFUNDED);
        }
        registrationDAO.updateStatus(runner.getId(), MarathonRunner.STATUS_CANCELLED);
        runner.setStatus(MarathonRunner.STATUS_CANCELLED);
        if (wasPaid) {
            PaymentAuditLog.refunded(runner, "userCancel");
        } else {
            PaymentAuditLog.cancelled(runner);
        }
        return wasPaid ? "refunded" : "cancelled";
    }

    /**
     * 管理员驳回报名：已支付的先退款再把状态置为驳回；未支付直接驳回。
     * 返回结果编码：rejected / refunded / refundFailed / notFound
     */
    public String rejectWithRefund(long id) throws SQLException {
        MarathonRunner runner = registrationDAO.findById(id);
        if (runner == null) {
            return "notFound";
        }
        boolean wasPaid = MarathonRunner.PAY_PAID.equals(runner.getPayStatus());
        if (wasPaid) {
            if (!refundQuietly(runner)) {
                PaymentAuditLog.refundFailed(runner);
                return "refundFailed";
            }
            registrationDAO.updatePayment(runner.getId(), runner.getOutTradeNo(),
                    MarathonRunner.PAY_REFUNDED, runner.getPayTime());
            runner.setPayStatus(MarathonRunner.PAY_REFUNDED);
        }
        registrationDAO.updateStatus(runner.getId(), MarathonRunner.STATUS_REJECTED);
        runner.setStatus(MarathonRunner.STATUS_REJECTED);
        if (wasPaid) {
            PaymentAuditLog.refunded(runner, "adminReject");
        }
        return wasPaid ? "refunded" : "rejected";
    }

    private void refundAndCancel(MarathonRunner runner) throws SQLException {
        boolean refunded = refundQuietly(runner);
        registrationDAO.updatePayment(runner.getId(), runner.getOutTradeNo(),
                MarathonRunner.PAY_REFUNDED, runner.getPayTime());
        registrationDAO.updateStatus(runner.getId(), MarathonRunner.STATUS_CANCELLED);
        runner.setPayStatus(MarathonRunner.PAY_REFUNDED);
        runner.setStatus(MarathonRunner.STATUS_CANCELLED);
        if (refunded) {
            PaymentAuditLog.refunded(runner, "capacityFull");
        } else {
            PaymentAuditLog.refundFailed(runner);
        }
    }

    private boolean refundQuietly(MarathonRunner runner) {
        try {
            return alipayService.refund(runner.getOutTradeNo(),
                    runner.getPayAmount() == null ? alipayService.getAmount()
                            : runner.getPayAmount().toPlainString());
        } catch (AlipayApiException e) {
            return false;
        }
    }
}
