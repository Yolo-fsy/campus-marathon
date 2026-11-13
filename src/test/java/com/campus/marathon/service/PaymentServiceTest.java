package com.campus.marathon.service;

import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;
import com.campus.marathon.service.RegistrationServiceCancelTest.InMemoryRegistrationDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 支付闭环（下单、到账、取消退款、名额控制）的单元测试。
 */
class PaymentServiceTest {

    /** 记录退款调用的假支付宝服务（mock 模式，不发起真实请求）。 */
    private static class FakeAlipayService extends AlipayService {
        private final List<String> refundCalls = new ArrayList<>();
        private boolean refundResult = true;

        FakeAlipayService() {
            super(new Properties());
        }

        @Override
        public boolean refund(String outTradeNo, String refundAmount) {
            refundCalls.add(outTradeNo + ":" + refundAmount);
            return refundResult;
        }
    }

    private RegistrationServiceCancelTest.InMemoryRegistrationDAO dao;
    private FakeAlipayService alipay;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        dao = new RegistrationServiceCancelTest.InMemoryRegistrationDAO();
        alipay = new FakeAlipayService();
        paymentService = new PaymentService(dao, alipay);
    }

    private MarathonRunner runner(long id, String studentId, String status, String payStatus) {
        MarathonRunner runner = new MarathonRunner();
        runner.setId(id);
        runner.setName("张三");
        runner.setStudentId(studentId);
        runner.setPhone("13800138000");
        runner.setEventCategory("MINI");
        runner.setStatus(status);
        runner.setPayStatus(payStatus);
        return runner;
    }

    @Test
    @DisplayName("创建订单生成订单号并写回记录")
    void createOrderGeneratesTradeNo() throws Exception {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);

        String outTradeNo = paymentService.createOrder(r);

        assertTrue(outTradeNo.startsWith("MR1"));
        assertEquals(outTradeNo, dao.get(1L).getOutTradeNo());
    }

    @Test
    @DisplayName("名额已满时创建订单直接拒绝（不超卖）")
    void createOrderRejectedWhenFull() {
        int capacity = EventCategory.fromCode("MINI").getCapacity();
        for (int i = 0; i < capacity; i++) {
            dao.save(runner(1000L + i, "9000000" + i, MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID));
        }
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);

        ValidationException ex = assertThrows(ValidationException.class, () -> paymentService.createOrder(r));
        assertTrue(ex.getFieldErrors().toString().contains("名额已满"));
    }

    @Test
    @DisplayName("支付到账：状态置为报名成功并占用名额")
    void handlePaidConfirmsRegistration() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);
        String outTradeNo = "MR1001";
        dao.updatePayment(1L, outTradeNo, MarathonRunner.PAY_UNPAID, null);

        PaymentService.PayOutcome outcome = paymentService.handlePaid(outTradeNo);

        assertEquals(PaymentService.PayResult.SUCCESS, outcome.getResult());
        assertEquals(MarathonRunner.STATUS_CONFIRMED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.PAY_PAID, dao.get(1L).getPayStatus());
        assertNotNull(dao.get(1L).getPayTime());
    }

    @Test
    @DisplayName("重复回调幂等：已支付订单再次到账返回 ALREADY_PAID")
    void handlePaidIsIdempotent() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_PAID, java.time.LocalDateTime.now());

        PaymentService.PayOutcome outcome = paymentService.handlePaid("MR1001");

        assertEquals(PaymentService.PayResult.ALREADY_PAID, outcome.getResult());
        assertEquals(0, alipay.refundCalls.size());
    }

    @Test
    @DisplayName("到账时名额已满：自动退款并取消报名")
    void handlePaidFullTriggersRefund() throws SQLException {
        int capacity = EventCategory.fromCode("MINI").getCapacity();
        for (int i = 0; i < capacity; i++) {
            dao.save(runner(1000L + i, "9000000" + i, MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID));
        }
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_UNPAID, null);

        PaymentService.PayOutcome outcome = paymentService.handlePaid("MR1001");

        assertEquals(PaymentService.PayResult.FULL_REFUNDED, outcome.getResult());
        assertEquals(1, alipay.refundCalls.size(), "应自动发起退款");
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.PAY_REFUNDED, dao.get(1L).getPayStatus());
    }

    @Test
    @DisplayName("未知订单号返回 NOT_FOUND")
    void handlePaidUnknownTradeNo() throws SQLException {
        assertEquals(PaymentService.PayResult.NOT_FOUND, paymentService.handlePaid("NOPE").getResult());
    }

    @Test
    @DisplayName("已支付报名取消：先退款再取消，返回 refunded")
    void cancelPaidRegistrationRefunds() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_PAID, java.time.LocalDateTime.now());

        String result = paymentService.cancelWithRefund("2023010001");

        assertEquals("refunded", result);
        assertEquals(1, alipay.refundCalls.size());
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.PAY_REFUNDED, dao.get(1L).getPayStatus());
    }

    @Test
    @DisplayName("未支付报名取消：不退款直接取消，返回 cancelled")
    void cancelUnpaidRegistrationSkipsRefund() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);

        String result = paymentService.cancelWithRefund("2023010001");

        assertEquals("cancelled", result);
        assertEquals(0, alipay.refundCalls.size(), "未支付不应触发退款");
        assertEquals(MarathonRunner.STATUS_CANCELLED, dao.get(1L).getStatus());
    }

    @Test
    @DisplayName("退款失败时报名不取消，返回 refundFailed")
    void cancelFailsWhenRefundFails() throws SQLException {
        alipay.refundResult = false;
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_PAID, java.time.LocalDateTime.now());

        String result = paymentService.cancelWithRefund("2023010001");

        assertEquals("refundFailed", result);
        assertEquals(MarathonRunner.STATUS_CONFIRMED, dao.get(1L).getStatus(), "退款失败不应取消报名");
    }

    @Test
    @DisplayName("无记录或已取消时返回 cancelFailed")
    void cancelFailsForMissingOrCancelled() throws SQLException {
        assertEquals("cancelFailed", paymentService.cancelWithRefund("2099999999"));

        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CANCELLED, MarathonRunner.PAY_REFUNDED);
        dao.save(r);
        assertEquals("cancelFailed", paymentService.cancelWithRefund("2023010001"));
    }

    @Test
    @DisplayName("驳回已支付报名：先退款再把状态置为驳回")
    void rejectPaidRegistrationRefundsFirst() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_PAID, java.time.LocalDateTime.now());

        String result = paymentService.rejectWithRefund(1L);

        assertEquals("refunded", result);
        assertEquals(1, alipay.refundCalls.size(), "应调用一次退款");
        MarathonRunner stored = dao.get(1L);
        assertEquals(MarathonRunner.STATUS_REJECTED, stored.getStatus());
        assertEquals(MarathonRunner.PAY_REFUNDED, stored.getPayStatus());
    }

    @Test
    @DisplayName("驳回未支付报名：不退款直接驳回")
    void rejectUnpaidRegistrationSkipsRefund() throws SQLException {
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_PENDING, MarathonRunner.PAY_UNPAID);
        dao.save(r);

        String result = paymentService.rejectWithRefund(1L);

        assertEquals("rejected", result);
        assertTrue(alipay.refundCalls.isEmpty(), "未支付不应发起退款");
        assertEquals(MarathonRunner.STATUS_REJECTED, dao.get(1L).getStatus());
        assertEquals(MarathonRunner.PAY_UNPAID, dao.get(1L).getPayStatus());
    }

    @Test
    @DisplayName("驳回时退款失败：保持原状态，不置为驳回")
    void rejectKeepsStatusWhenRefundFails() throws SQLException {
        alipay.refundResult = false;
        MarathonRunner r = runner(1L, "2023010001", MarathonRunner.STATUS_CONFIRMED, MarathonRunner.PAY_PAID);
        dao.save(r);
        dao.updatePayment(1L, "MR1001", MarathonRunner.PAY_PAID, java.time.LocalDateTime.now());

        String result = paymentService.rejectWithRefund(1L);

        assertEquals("refundFailed", result);
        MarathonRunner stored = dao.get(1L);
        assertEquals(MarathonRunner.STATUS_CONFIRMED, stored.getStatus(), "退款失败不应驳回");
        assertEquals(MarathonRunner.PAY_PAID, stored.getPayStatus());
    }

    @Test
    @DisplayName("驳回不存在的记录返回 notFound")
    void rejectMissingReturnsNotFound() throws SQLException {
        assertEquals("notFound", paymentService.rejectWithRefund(999L));
    }
}
