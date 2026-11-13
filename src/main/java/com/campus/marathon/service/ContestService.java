package com.campus.marathon.service;

import com.campus.marathon.dao.CriterionDAO;
import com.campus.marathon.dao.ContestDAO;
import com.campus.marathon.dao.ContestantDAO;
import com.campus.marathon.dao.impl.CriterionDAOImpl;
import com.campus.marathon.dao.impl.ContestDAOImpl;
import com.campus.marathon.dao.impl.ContestantDAOImpl;
import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Criterion;

import java.sql.SQLException;
import java.util.List;

/**
 * 竞赛生命周期 Service：管理竞赛状态流转、维度权重校验、评分阶段限制。
 */
public class ContestService {

    private final ContestDAO contestDAO;
    private final CriterionDAO criterionDAO;
    private final ContestantDAO contestantDAO;

    public ContestService() {
        this.contestDAO = new ContestDAOImpl();
        this.criterionDAO = new CriterionDAOImpl();
        this.contestantDAO = new ContestantDAOImpl();
    }

    /** 构造注入（测试用） */
    public ContestService(ContestDAO contestDAO, CriterionDAO criterionDAO, ContestantDAO contestantDAO) {
        this.contestDAO = contestDAO;
        this.criterionDAO = criterionDAO;
        this.contestantDAO = contestantDAO;
    }

    // ========== 竞赛生命周期 ==========

    /** 新建竞赛，默认 DRAFT 状态 */
    public Contest create(String name, String description) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("竞赛名称不能为空");
        }
        Contest contest = new Contest();
        contest.setName(name.trim());
        contest.setDescription(description == null ? null : description.trim());
        contest.setStatus(Contest.STATUS_DRAFT);
        long id = contestDAO.create(contest);
        contest.setId(id);
        return contest;
    }

    /** 更新基本信息（仅 DRAFT 状态允许） */
    public void updateBasicInfo(long contestId, String name, String description) throws SQLException {
        Contest c = requireContest(contestId);
        if (!c.isDraft()) {
            throw new IllegalStateException("仅草稿状态可以修改竞赛基本信息");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("竞赛名称不能为空");
        }
        c.setName(name.trim());
        c.setDescription(description == null ? null : description.trim());
        contestDAO.update(c);
    }

    /**
     * 启动评审（DRAFT → JUDGING）：
     * - 校验 criteria 权重和 = 100
     * - 至少需要一个评审
     */
    public void startJudging(long contestId) throws SQLException {
        Contest c = requireContest(contestId);
        if (!c.isDraft()) {
            throw new IllegalStateException("只有 DRAFT 状态才能启动评审");
        }
        validateCriterionWeights(contestId);
        boolean ok = contestDAO.updateStatus(contestId, Contest.STATUS_JUDGING);
        if (!ok) {
            throw new IllegalStateException("启动评审失败，竞赛不存在");
        }
    }

    /**
     * 结束评审（JUDGING → FINISHED）。
     */
    public void finishContest(long contestId) throws SQLException {
        Contest c = requireContest(contestId);
        if (!c.isJudging()) {
            throw new IllegalStateException("只有 JUDGING 状态才能结束");
        }
        boolean ok = contestDAO.updateStatus(contestId, Contest.STATUS_FINISHED);
        if (!ok) {
            throw new IllegalStateException("结束评审失败");
        }
    }

    /** 回退到 DRAFT（JUDGING → DRAFT，用于中途补充评审或维度） */
    public void revertToDraft(long contestId) throws SQLException {
        Contest c = requireContest(contestId);
        if (!c.isJudging()) {
            throw new IllegalStateException("只有 JUDGING 状态才能回退到 DRAFT");
        }
        boolean ok = contestDAO.updateStatus(contestId, Contest.STATUS_DRAFT);
        if (!ok) {
            throw new IllegalStateException("回退失败");
        }
    }

    // ========== 维度管理 ==========

    /** 创建维度：DRAFT 状态允许，权重 0-100 */
    public long addCriterion(long contestId, String name, int weight, String description) throws SQLException {
        Contest c = requireContest(contestId);
        if (c.isJudging() || c.isFinished()) {
            throw new IllegalStateException("评分阶段或已结束时不能添加维度");
        }
        validateWeightRange(weight);
        Criterion crit = new Criterion();
        crit.setContestId(contestId);
        crit.setName(name == null ? "" : name.trim());
        crit.setWeight(weight);
        crit.setDescription(description == null ? null : description.trim());
        return criterionDAO.create(crit);
    }

    /** 修改维度（仅 DRAFT 状态） */
    public void updateCriterion(long contestId, long criterionId, String name, int weight, String description)
            throws SQLException {
        Contest c = requireContest(contestId);
        if (c.isJudging() || c.isFinished()) {
            throw new IllegalStateException("评分阶段禁止修改评分维度");
        }
        validateWeightRange(weight);
        Criterion crit = new Criterion();
        crit.setId(criterionId);
        crit.setContestId(contestId);
        crit.setName(name == null ? "" : name.trim());
        crit.setWeight(weight);
        crit.setDescription(description == null ? null : description.trim());
        boolean ok = criterionDAO.update(crit);
        if (!ok) {
            throw new IllegalStateException("修改维度失败，可能记录不存在");
        }
    }

    /** 删除维度（仅 DRAFT 状态） */
    public void deleteCriterion(long contestId, long criterionId) throws SQLException {
        Contest c = requireContest(contestId);
        if (c.isJudging() || c.isFinished()) {
            throw new IllegalStateException("评分阶段禁止删除评分维度");
        }
        boolean ok = criterionDAO.delete(criterionId, contestId);
        if (!ok) {
            throw new IllegalStateException("删除维度失败，可能记录不存在");
        }
    }

    // ========== 参赛者管理 ==========

    /** 添加参赛者 */
    public long addContestant(long contestId, String name, String studentId, String teamName) throws SQLException {
        requireContest(contestId);
        com.campus.marathon.model.Contestant ct = new com.campus.marathon.model.Contestant();
        ct.setContestId(contestId);
        ct.setName(name == null ? null : name.trim());
        ct.setStudentId(studentId == null ? null : studentId.trim());
        ct.setTeamName(teamName == null ? null : teamName.trim());
        return contestantDAO.create(ct);
    }

    public void removeContestant(long contestId, long contestantId) throws SQLException {
        boolean ok = contestantDAO.delete(contestantId, contestId);
        if (!ok) {
            throw new IllegalStateException("删除参赛者失败");
        }
    }

    // ========== 查询 ==========

    public Contest getContest(long id) throws SQLException {
        return contestDAO.findById(id);
    }

    public List<Contest> listAll() throws SQLException {
        return contestDAO.listAll();
    }

    public List<Contest> listByStatus(String status) throws SQLException {
        return contestDAO.listByStatus(status);
    }

    public List<Criterion> listCriteria(long contestId) throws SQLException {
        return criterionDAO.listByContest(contestId);
    }

    public List<com.campus.marathon.model.Contestant> listContestants(long contestId) throws SQLException {
        return contestantDAO.listByContest(contestId);
    }

    // ========== 内部工具 ==========

    /** 校验 criteria 权重和 = 100；为空时抛异常 */
    public void validateCriterionWeights(long contestId) throws SQLException {
        List<Criterion> criteria = criterionDAO.listByContest(contestId);
        if (criteria.isEmpty()) {
            throw new IllegalStateException("至少需要一个评分维度才能启动评审");
        }
        int sum = 0;
        for (Criterion c : criteria) {
            sum += c.getWeight();
        }
        if (sum != 100) {
            throw new IllegalStateException("所有评分维度的权重之和必须为 100，当前为 " + sum);
        }
    }

    private void validateWeightRange(int weight) {
        if (weight < 0 || weight > 100) {
            throw new IllegalArgumentException("权重必须在 0 到 100 之间");
        }
    }

    private Contest requireContest(long contestId) throws SQLException {
        Contest c = contestDAO.findById(contestId);
        if (c == null) {
            throw new IllegalArgumentException("竞赛不存在，id = " + contestId);
        }
        return c;
    }
}
