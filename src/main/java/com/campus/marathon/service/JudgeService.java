package com.campus.marathon.service;

import com.campus.marathon.dao.JudgeDAO;
import com.campus.marathon.dao.impl.JudgeDAOImpl;
import com.campus.marathon.model.Judge;
import com.campus.marathon.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * 评审账号 Service：账号增删、登录验证。
 * 每个竞赛的评审独立管理，但 username 全局唯一约束也可以按需调整。
 */
public class JudgeService {

    private final JudgeDAO judgeDAO;

    public JudgeService() {
        this.judgeDAO = new JudgeDAOImpl();
    }

    /** 构造注入（测试用） */
    public JudgeService(JudgeDAO judgeDAO) {
        this.judgeDAO = judgeDAO;
    }

    /** 为某竞赛添加评审账号 */
    public Judge addJudge(long contestId, String username, String displayName, String password) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.length() < 3) {
            throw new IllegalArgumentException("密码至少 3 位");
        }
        // 同一竞赛下不允许重复 username
        Judge existing = judgeDAO.findByUsernameAndContest(username.trim(), contestId);
        if (existing != null) {
            throw new IllegalStateException("该竞赛下已存在同名评审：" + username);
        }
        String salt = PasswordUtil.generateSalt();
        String credential = PasswordUtil.hash(salt, password);

        Judge judge = new Judge();
        judge.setContestId(contestId);
        judge.setUsername(username.trim());
        judge.setDisplayName(displayName == null || displayName.trim().isEmpty() ? username.trim() : displayName.trim());
        judge.setCredential(credential);
        judge.setSalt(salt);

        long id = judgeDAO.create(judge);
        judge.setId(id);
        return judge;
    }

    /** 移除评审 */
    public void removeJudge(long contestId, long judgeId) throws SQLException {
        boolean ok = judgeDAO.removeJudge(contestId, judgeId);
        if (!ok) {
            throw new IllegalStateException("移除评审失败，可能记录不存在");
        }
    }

    /** 列出某竞赛的所有评审 */
    public List<Judge> listByContest(long contestId) throws SQLException {
        return judgeDAO.listByContest(contestId);
    }

    /** 按 username + contestId 精确查询 */
    public Judge findByUsernameAndContest(String username, long contestId) throws SQLException {
        return judgeDAO.findByUsernameAndContest(username, contestId);
    }

    /**
     * 评审登录验证。
     * @return 验证通过返回 Judge；否则返回 null
     */
    public Judge authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }
        // 先按 username 查（评审账号在某场竞赛下独立，但一个评审可能跨多场；
        // 这里默认取第一条，后续按 contestId 精确）
        Judge judge = judgeDAO.findByUsername(username);
        if (judge == null) {
            return null;
        }
        if (!PasswordUtil.verify(judge.getSalt(), password, judge.getCredential())) {
            return null;
        }
        return judge;
    }

    /** 精确到某竞赛的登录 */
    public Judge authenticateInContest(String username, String password, long contestId) throws SQLException {
        Judge judge = judgeDAO.findByUsernameAndContest(username, contestId);
        if (judge == null) {
            return null;
        }
        return PasswordUtil.verify(judge.getSalt(), password, judge.getCredential()) ? judge : null;
    }

    /** 重置评审密码 */
    public void resetPassword(long judgeId, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.length() < 3) {
            throw new IllegalArgumentException("密码至少 3 位");
        }
        String salt = PasswordUtil.generateSalt();
        String credential = PasswordUtil.hash(salt, newPassword);
        boolean ok = judgeDAO.updateCredential(judgeId, credential, salt);
        if (!ok) {
            throw new IllegalStateException("重置密码失败");
        }
    }
}
