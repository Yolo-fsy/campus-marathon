package com.campus.marathon.service;

import com.campus.marathon.dao.RegistrationDAO;
import com.campus.marathon.dao.impl.RegistrationDAOImpl;
import com.campus.marathon.model.DashboardStats;
import com.campus.marathon.model.EventCategory;
import com.campus.marathon.model.MarathonRunner;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 报名业务逻辑：数据校验、学号查重、名额控制。
 */
public class RegistrationService {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z·\\s]{2,20}$");
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{8,12}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final RegistrationDAO registrationDAO;

    public RegistrationService() {
        this(new RegistrationDAOImpl());
    }

    public RegistrationService(RegistrationDAO registrationDAO) {
        this.registrationDAO = registrationDAO;
    }

    public List<MarathonRunner> search(String keyword, String category, String status, String payStatus)
            throws SQLException {
        String categoryFilter = EventCategory.isValid(category)
                ? EventCategory.fromCode(category).getCode() : null;
        String statusFilter = isKnownStatus(status) ? status.trim().toUpperCase() : null;
        String payFilter = isKnownPayStatus(payStatus) ? payStatus.trim().toUpperCase() : null;
        return registrationDAO.findAll(keyword, categoryFilter, statusFilter, payFilter);
    }

    public MarathonRunner findById(long id) throws SQLException {
        return registrationDAO.findById(id);
    }

    public MarathonRunner findByStudentId(String studentId) throws SQLException {
        return registrationDAO.findByStudentId(studentId);
    }

    /**
     * 管理员审核：通过或驳回报名。
     */
    public boolean review(long id, String status) throws ValidationException, SQLException {
        MarathonRunner runner = registrationDAO.findById(id);
        if (runner == null) {
            return false;
        }
        if (!MarathonRunner.STATUS_CONFIRMED.equals(status)
                && !MarathonRunner.STATUS_REJECTED.equals(status)) {
            throw new IllegalArgumentException("不支持的审核状态：" + status);
        }
        if (MarathonRunner.STATUS_CONFIRMED.equals(status)) {
            EventCategory category = EventCategory.fromCode(runner.getEventCategory());
            int used = registrationDAO.countConfirmedByCategory(category.getCode(), id);
            if (used >= category.getCapacity()) {
                Map<String, String> errors = new LinkedHashMap<>();
                errors.put("status", category.getLabel() + "名额已满（限额 "
                        + category.getCapacity() + " 人），无法通过审核");
                throw new ValidationException(errors);
            }
            // 原子确认兜底：两个管理员并发审核同一项目时，锁+数据库原子更新保证后提交者失败而非超卖
            boolean confirmed = CapacityGuard.confirmWithCapacity(
                    registrationDAO, id, category.getCode(), category.getCapacity());
            if (!confirmed) {
                Map<String, String> errors = new LinkedHashMap<>();
                errors.put("status", category.getLabel() + "名额刚被占满（限额 "
                        + category.getCapacity() + " 人），无法通过审核");
                throw new ValidationException(errors);
            }
            return true;
        }
        return registrationDAO.updateStatus(id, status);
    }

    /**
     * 参赛者取消本人报名：保留报名记录用于赛后数据沉淀，仅将状态置为已取消并释放名额。
     */
    public boolean cancelOwn(String studentId) throws SQLException {
        MarathonRunner runner = registrationDAO.findByStudentId(studentId);
        if (runner == null) {
            return false;
        }
        if (MarathonRunner.STATUS_CANCELLED.equals(runner.getStatus())) {
            return false;
        }
        return registrationDAO.updateStatus(runner.getId(), MarathonRunner.STATUS_CANCELLED);
    }

    public DashboardStats loadStats() throws SQLException {
        // 读取统计前先清理超时未支付订单，保证「报名成功」卡片与真实占位一致
        registrationDAO.cancelExpiredUnpaid(LocalDateTime.now()
                .minusHours(MarathonRunner.PAYMENT_DEADLINE_HOURS));
        return registrationDAO.loadStats();
    }

    public void create(MarathonRunner runner) throws ValidationException, SQLException {
        validate(runner, true);
        registrationDAO.create(runner);
    }

    /**
     * 在线报名：学号无记录时新建；已取消的记录在原行上重新报名（状态与支付信息重置）；
     * 其他情况视为重复报名。返回报名记录主键。
     */
    public long applyOrReapply(MarathonRunner runner) throws ValidationException, SQLException {
        MarathonRunner existing = registrationDAO.findByStudentId(trim(runner.getStudentId()));
        if (existing == null) {
            create(runner);
            return runner.getId();
        }
        if (!MarathonRunner.STATUS_CANCELLED.equals(existing.getStatus())) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("studentId", "该学号已有报名记录，如尚未完成支付请登录后在「我的报名」继续支付");
            throw new ValidationException(errors);
        }
        runner.setId(existing.getId());
        validate(runner, false);
        registrationDAO.reapply(runner);
        return existing.getId();
    }

    public boolean update(MarathonRunner runner) throws ValidationException, SQLException {
        validate(runner, false);
        return registrationDAO.update(runner);
    }

    public boolean delete(long id) throws SQLException {
        return registrationDAO.delete(id);
    }

    private void validate(MarathonRunner runner, boolean creating) throws ValidationException, SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        runner.setName(trim(runner.getName()));
        runner.setStudentId(trim(runner.getStudentId()));
        runner.setPhone(trim(runner.getPhone()));
        runner.setCollege(trim(runner.getCollege()));
        runner.setRemark(trim(runner.getRemark()));

        if (runner.getName().isEmpty()) {
            errors.put("name", "请填写参赛者姓名");
        } else if (!NAME_PATTERN.matcher(runner.getName()).matches()) {
            errors.put("name", "姓名为 2-20 个字符，仅支持中英文字母");
        }

        if (runner.getStudentId().isEmpty()) {
            errors.put("studentId", "请填写学号");
        } else if (!STUDENT_ID_PATTERN.matcher(runner.getStudentId()).matches()) {
            errors.put("studentId", "学号应为 8-12 位数字");
        }

        if (runner.getPhone().isEmpty()) {
            errors.put("phone", "请填写联系电话");
        } else if (!PHONE_PATTERN.matcher(runner.getPhone()).matches()) {
            errors.put("phone", "请输入 11 位有效手机号");
        }

        if (runner.getCollege().length() > 40) {
            errors.put("college", "学院名称不超过 40 个字符");
        }

        String category = trim(runner.getEventCategory());
        if (!EventCategory.isValid(category)) {
            errors.put("eventCategory", "请选择有效的参赛项目");
        } else {
            runner.setEventCategory(EventCategory.fromCode(category).getCode());
        }

        String status = trim(runner.getStatus());
        if (status.isEmpty()) {
            status = MarathonRunner.STATUS_PENDING;
        }
        status = status.toUpperCase();
        if (!isKnownStatus(status)) {
            errors.put("status", "报名状态不合法");
        }
        runner.setStatus(status);

        if (runner.getRemark().length() > 120) {
            errors.put("remark", "备注不超过 120 个字符");
        }

        if (errors.isEmpty()) {
            Long excludeId = creating ? null : runner.getId();
            if (registrationDAO.existsByStudentId(runner.getStudentId(), excludeId)) {
                errors.put("studentId", "该学号已有报名记录，请勿重复报名");
            }

            if (MarathonRunner.STATUS_CONFIRMED.equals(status)) {
                EventCategory selected = EventCategory.fromCode(runner.getEventCategory());
                int used = registrationDAO.countConfirmedByCategory(selected.getCode(), excludeId);
                if (used >= selected.getCapacity()) {
                    errors.put("status", selected.getLabel() + "名额已满（限额 "
                            + selected.getCapacity() + " 人），请改报其他项目或保持待审核");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private boolean isKnownStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return false;
        }
        String value = status.trim().toUpperCase();
        return MarathonRunner.STATUS_PENDING.equals(value)
                || MarathonRunner.STATUS_CONFIRMED.equals(value)
                || MarathonRunner.STATUS_REJECTED.equals(value)
                || MarathonRunner.STATUS_CANCELLED.equals(value);
    }

    private boolean isKnownPayStatus(String payStatus) {
        if (payStatus == null || payStatus.trim().isEmpty()) {
            return false;
        }
        String value = payStatus.trim().toUpperCase();
        return MarathonRunner.PAY_UNPAID.equals(value)
                || MarathonRunner.PAY_PAID.equals(value)
                || MarathonRunner.PAY_REFUNDED.equals(value);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
