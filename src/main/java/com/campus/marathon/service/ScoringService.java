package com.campus.marathon.service;

import com.campus.marathon.dao.CriterionDAO;
import com.campus.marathon.dao.ContestantDAO;
import com.campus.marathon.dao.ScoreDAO;
import com.campus.marathon.dao.impl.CriterionDAOImpl;
import com.campus.marathon.dao.impl.ContestantDAOImpl;
import com.campus.marathon.dao.impl.ScoreDAOImpl;
import com.campus.marathon.model.ContestRank;
import com.campus.marathon.model.Contestant;
import com.campus.marathon.model.Criterion;
import com.campus.marathon.model.Score;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评分与排名 Service：
 * - submitScore 防重复（覆盖更新）
 * - computeContestRank 实时计算加权总分、同分并列名次
 */
public class ScoringService {

    private final ScoreDAO scoreDAO;
    private final ContestantDAO contestantDAO;
    private final CriterionDAO criterionDAO;

    public ScoringService() {
        this.scoreDAO = new ScoreDAOImpl();
        this.contestantDAO = new ContestantDAOImpl();
        this.criterionDAO = new CriterionDAOImpl();
    }

    /** 构造注入（测试用） */
    public ScoringService(ScoreDAO scoreDAO, ContestantDAO contestantDAO, CriterionDAO criterionDAO) {
        this.scoreDAO = scoreDAO;
        this.contestantDAO = contestantDAO;
        this.criterionDAO = criterionDAO;
    }

    /**
     * 提交评分，自动覆盖之前的同 (judge, contestant, criterion) 记录。
     * @param score 0-10
     */
    public void submitScore(long judgeId, long contestantId, long criterionId,
                            BigDecimal score, String comment) throws SQLException {
        if (score == null) {
            throw new IllegalArgumentException("score 不能为空");
        }
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.TEN;
        if (score.compareTo(min) < 0 || score.compareTo(max) > 0) {
            throw new IllegalArgumentException("score 必须在 0 到 10 之间");
        }
        scoreDAO.submit(judgeId, contestantId, criterionId, score, comment);
    }

    /** 查询某评审对某参赛者已提交的所有评分（表单回显） */
    public List<Score> listScoresByJudgeAndContestant(long judgeId, long contestantId) throws SQLException {
        return scoreDAO.listByJudgeAndContestant(judgeId, contestantId);
    }

    /**
     * 实时计算某场竞赛的排名。
     *
     * <p>计算逻辑：
     * <ol>
     *   <li>遍历所有参赛者</li>
     *   <li>对每位参赛者的所有评分，按 criterion.weight / 100 加权 → 求和</li>
     *   <li>按总分降序排序</li>
     *   <li>同分并列：相同总分者同一名次，下一名次跳过（1, 2, 2, 4 模式）</li>
     * </ol>
     *
     * @return 从第一名开始的排名列表（已按名次升序）
     */
    public List<ContestRank> computeContestRank(long contestId) throws SQLException {
        // 1. 加载基础数据
        List<Contestant> contestants = contestantDAO.listByContest(contestId);
        List<Criterion> criteria = criterionDAO.listByContest(contestId);
        List<Score> allScores = scoreDAO.listByContest(contestId);

        // criterionId → weight
        Map<Long, Integer> criterionWeight = new LinkedHashMap<>();
        for (Criterion c : criteria) {
            criterionWeight.put(c.getId(), c.getWeight());
        }

        // 2. 按 contestantId 分组聚合
        // contestantId → 该参赛者的所有 Score
        Map<Long, List<Score>> perContestant = new HashMap<>();
        for (Score s : allScores) {
            perContestant.computeIfAbsent(s.getContestantId(), k -> new ArrayList<>()).add(s);
        }

        // 同时统计某 contestantId 有哪些评审参与打分
        // contestantId → Set<judgeId>
        Map<Long, java.util.Set<Long>> judgeSet = new HashMap<>();
        for (Score s : allScores) {
            judgeSet.computeIfAbsent(s.getContestantId(), k -> new java.util.HashSet<>()).add(s.getJudgeId());
        }

        // 3. 为每位参赛者计算加权总分
        BigDecimal HUNDRED = new BigDecimal("100");
        List<ContestRank> ranks = new ArrayList<>();
        for (Contestant ct : contestants) {
            List<Score> scores = perContestant.getOrDefault(ct.getId(), new ArrayList<>());
            java.util.Set<Long> judges = judgeSet.getOrDefault(ct.getId(), new java.util.HashSet<>());

            BigDecimal weightedTotal = BigDecimal.ZERO;
            BigDecimal rawSum = BigDecimal.ZERO;
            for (Score s : scores) {
                Integer weight = criterionWeight.get(s.getCriterionId());
                BigDecimal w = (weight == null) ? BigDecimal.ZERO : new BigDecimal(weight);
                // score * weight / 100  → 累加到加权总分
                BigDecimal weighted = s.getScore().multiply(w).divide(HUNDRED, 6, RoundingMode.HALF_UP);
                weightedTotal = weightedTotal.add(weighted);
                rawSum = rawSum.add(s.getScore());
            }

            int judgeCount = judges.size();
            BigDecimal avgPerJudge;
            if (judgeCount > 0) {
                avgPerJudge = rawSum.divide(new BigDecimal(judgeCount), 2, RoundingMode.HALF_UP);
            } else {
                avgPerJudge = BigDecimal.ZERO;
            }

            ranks.add(new ContestRank(ct, weightedTotal, 0, avgPerJudge, judgeCount));
        }

        // 4. 按加权总分降序排序；同分按 id 升序保持稳定
        ranks.sort((a, b) -> {
            BigDecimal at = a.getTotalScore() == null ? BigDecimal.ZERO : a.getTotalScore();
            BigDecimal bt = b.getTotalScore() == null ? BigDecimal.ZERO : b.getTotalScore();
            int cmp = bt.compareTo(at); // 降序
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(a.getContestant().getId(), b.getContestant().getId());
        });

        // 5. 计算名次（同分同名次、跳过后续，即 1,1,3 模式）
        int rankIndex = 0;
        int actualRank = 0;
        BigDecimal lastScore = null;
        for (ContestRank r : ranks) {
            rankIndex++;
            BigDecimal s = r.getTotalScore() == null ? BigDecimal.ZERO : r.getTotalScore();
            if (lastScore == null || s.compareTo(lastScore) != 0) {
                actualRank = rankIndex;
                lastScore = s;
            }
            r.setRank(actualRank);
        }

        return ranks;
    }

    /** 查询某参赛者在全部维度上的评分详情（详情页用） */
    public Map<Criterion, Score> listContestantScoresByCriteria(long contestantId) throws SQLException {
        List<Score> scores = scoreDAO.listByContestant(contestantId);
        List<Criterion> criteria = criterionDAO.listByContest(contestantId); // 先查出来，下面用 contestId 过滤

        // 但 criterionDAO.listByContest 需要 contestId，这里我们从 contestant 反查 contestId
        Contestant ct = contestantDAO.findById(contestantId);
        if (ct == null) {
            return new LinkedHashMap<>();
        }
        criteria = criterionDAO.listByContest(ct.getContestId());

        Map<Long, Criterion> critMap = new LinkedHashMap<>();
        for (Criterion c : criteria) {
            critMap.put(c.getId(), c);
        }

        Map<Criterion, Score> result = new LinkedHashMap<>();
        // 先按 criterion 排序好，即使某些维度没有评分也放进去，值为 null
        for (Criterion c : criteria) {
            result.put(c, null);
        }
        for (Score s : scores) {
            Criterion c = critMap.get(s.getCriterionId());
            if (c != null) {
                result.put(c, s);
            }
        }
        return result;
    }
}
