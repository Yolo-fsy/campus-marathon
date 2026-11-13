package com.campus.marathon.service;

import com.campus.marathon.dao.CriterionDAO;
import com.campus.marathon.dao.ContestantDAO;
import com.campus.marathon.dao.ScoreDAO;
import com.campus.marathon.model.ContestRank;
import com.campus.marathon.model.Contestant;
import com.campus.marathon.model.Criterion;
import com.campus.marathon.model.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScoringService 单元测试：同分并列、权重计算、防重复覆盖。
 * 使用内存假 DAO。
 */
class ScoringServiceTest {

    /** 内存假 ContestantDAO */
    static class InMemoryContestantDAO implements ContestantDAO {
        private final Map<Long, Contestant> store = new HashMap<>();

        void save(Contestant c) { store.put(c.getId(), c); }

        @Override
        public long create(Contestant contestant) { throw new UnsupportedOperationException(); }
        @Override
        public Contestant findById(long id) { return store.get(id); }
        @Override
        public List<Contestant> listByContest(long contestId) {
            List<Contestant> list = new ArrayList<>();
            for (Contestant c : store.values()) {
                if (c.getContestId() == contestId) list.add(c);
            }
            return list;
        }
        @Override
        public boolean delete(long id, long contestId) { return store.remove(id) != null; }
        @Override
        public boolean update(Contestant contestant) { store.put(contestant.getId(), contestant); return true; }
    }

    /** 内存假 CriterionDAO */
    static class InMemoryCriterionDAO implements CriterionDAO {
        private final Map<Long, Criterion> store = new HashMap<>();

        void save(Criterion c) { store.put(c.getId(), c); }

        @Override
        public long create(Criterion criterion) { throw new UnsupportedOperationException(); }
        @Override
        public List<Criterion> listByContest(long contestId) {
            List<Criterion> list = new ArrayList<>();
            for (Criterion c : store.values()) {
                if (c.getContestId() == contestId) list.add(c);
            }
            return list;
        }
        @Override
        public boolean updateWeight(long id, long contestId, int weight) { return false; }
        @Override
        public boolean update(Criterion criterion) { return false; }
        @Override
        public boolean delete(long id, long contestId) { return false; }
        @Override
        public boolean deleteByContest(long contestId) { return false; }
    }

    /** 内存假 ScoreDAO */
    static class InMemoryScoreDAO implements ScoreDAO {
        private final List<Score> store = new ArrayList<>();
        /** 记录每次 submit 的调用参数（last 覆盖更新） */
        Score lastSubmitted;

        @Override
        public synchronized void submit(long judgeId, long contestantId, long criterionId,
                                        BigDecimal score, String comment) throws SQLException {
            // 模拟唯一约束覆盖：先删掉同键的，再插入新的
            store.removeIf(s -> s.getJudgeId() == judgeId
                    && s.getContestantId() == contestantId
                    && s.getCriterionId() == criterionId);
            Score s = new Score();
            s.setJudgeId(judgeId);
            s.setContestantId(contestantId);
            s.setCriterionId(criterionId);
            s.setScore(score);
            s.setComment(comment);
            store.add(s);
            lastSubmitted = s;
        }

        @Override
        public List<Score> listByContestant(long contestantId) {
            List<Score> out = new ArrayList<>();
            for (Score s : store) {
                if (s.getContestantId() == contestantId) out.add(s);
            }
            return out;
        }

        @Override
        public List<Score> listByContest(long contestId) {
            // 简化：直接返回全部，因为我们的 contestants 都在同一个 contest 下
            return new ArrayList<>(store);
        }

        @Override
        public List<Score> listByJudgeAndContestant(long judgeId, long contestantId) {
            List<Score> out = new ArrayList<>();
            for (Score s : store) {
                if (s.getJudgeId() == judgeId && s.getContestantId() == contestantId) out.add(s);
            }
            return out;
        }

        @Override
        public boolean deleteByContestant(long contestantId) {
            return store.removeIf(s -> s.getContestantId() == contestantId);
        }
    }

    private InMemoryContestantDAO contestantDAO;
    private InMemoryCriterionDAO criterionDAO;
    private InMemoryScoreDAO scoreDAO;
    private ScoringService service;

    @BeforeEach
    void setUp() {
        contestantDAO = new InMemoryContestantDAO();
        criterionDAO = new InMemoryCriterionDAO();
        scoreDAO = new InMemoryScoreDAO();
        service = new ScoringService(scoreDAO, contestantDAO, criterionDAO);

        // 构造 1 场竞赛 (contestId=1)
        // 3 维度：技术难度 30 / 创新性 40 / 完成度 30
        Criterion c1 = new Criterion(); c1.setId(1); c1.setContestId(1); c1.setName("技术难度"); c1.setWeight(30);
        Criterion c2 = new Criterion(); c2.setId(2); c2.setContestId(1); c2.setName("创新性");   c2.setWeight(40);
        Criterion c3 = new Criterion(); c3.setId(3); c3.setContestId(1); c3.setName("完成度");   c3.setWeight(30);
        criterionDAO.save(c1); criterionDAO.save(c2); criterionDAO.save(c3);

        // 2 参赛者
        Contestant a = new Contestant(); a.setId(1); a.setContestId(1); a.setName("甲");
        Contestant b = new Contestant(); b.setId(2); b.setContestId(1); b.setName("乙");
        contestantDAO.save(a); contestantDAO.save(b);
    }

    @Test
    @DisplayName("权重计算正确：甲 8x30 + 9x40 + 7x30 = 240 + 360 + 210 = 810, /100 = 8.10")
    void weightedTotalCalculatedCorrectly() throws SQLException {
        // judge 1 给甲打分
        service.submitScore(100L, 1L, 1L, bd("8"),  null);
        service.submitScore(100L, 1L, 2L, bd("9"),  null);
        service.submitScore(100L, 1L, 3L, bd("7.5"), null);

        List<ContestRank> ranks = service.computeContestRank(1L);
        assertEquals(2, ranks.size());
        ContestRank first = ranks.get(0);
        // 8*30/100=2.4, 9*40/100=3.6, 7.5*30/100=2.25 → 合计 8.25
        assertEquals(bd("8.25"), first.getTotalScore().setScale(2));
    }

    @Test
    @DisplayName("同分并列：两参赛者得分完全相同，都为第 1 名")
    void tiedContestantsShareSameRank() throws SQLException {
        // 甲和乙完全一样的 3 维分数
        BigDecimal score = bd("8");
        service.submitScore(100L, 1L, 1L, score, null);
        service.submitScore(100L, 1L, 2L, score, null);
        service.submitScore(100L, 1L, 3L, score, null);
        service.submitScore(100L, 2L, 1L, score, null);
        service.submitScore(100L, 2L, 2L, score, null);
        service.submitScore(100L, 2L, 3L, score, null);

        List<ContestRank> ranks = service.computeContestRank(1L);
        assertEquals(2, ranks.size());
        assertEquals(1, ranks.get(0).getRank());
        assertEquals(1, ranks.get(1).getRank());
    }

    @Test
    @DisplayName("同分并列后跳过后续名次：1,1,3 模式")
    void skipsRankAfterTie() throws SQLException {
        // 加第 3 名
        Contestant c = new Contestant(); c.setId(3); c.setContestId(1); c.setName("丙");
        contestantDAO.save(c);

        // 甲乙同分 8，丙略低
        for (long crit : new long[]{1, 2, 3}) {
            service.submitScore(100L, 1L, crit, bd("8"), null);
            service.submitScore(100L, 2L, crit, bd("8"), null);
            service.submitScore(100L, 3L, crit, bd("6"), null);
        }

        List<ContestRank> ranks = service.computeContestRank(1L);
        assertEquals(3, ranks.size());
        assertEquals(1, ranks.get(0).getRank(), "甲 = 1");
        assertEquals(1, ranks.get(1).getRank(), "乙 同分 = 1");
        assertEquals(3, ranks.get(2).getRank(), "丙 跳过 2 = 3");
    }

    @Test
    @DisplayName("防重复评分覆盖更新：同一 judge+contestant+criterion 覆盖最后一次")
    void submitScoreOverwritesPrevious() throws SQLException {
        service.submitScore(100L, 1L, 1L, bd("5"), "第一次");
        service.submitScore(100L, 1L, 1L, bd("9"), "覆盖");

        assertEquals(1, scoreDAO.store.size());
        assertEquals(bd("9"), scoreDAO.store.get(0).getScore());
        assertEquals("覆盖", scoreDAO.store.get(0).getComment());
    }

    @Test
    @DisplayName("分数边界校验：>10 或 <0 抛异常")
    void scoreRangeValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                service.submitScore(100L, 1L, 1L, bd("10.1"), null));
        assertThrows(IllegalArgumentException.class, () ->
                service.submitScore(100L, 1L, 1L, bd("-0.1"), null));
        // 边界值 0 和 10 合法
        assertDoesNotThrow(() -> service.submitScore(100L, 1L, 1L, bd("0"), null));
        assertDoesNotThrow(() -> service.submitScore(100L, 1L, 1L, bd("10"), null));
    }

    @Test
    @DisplayName("多位评审参与时平均分按评审数算")
    void averagePerJudgeCalculated() throws SQLException {
        // 甲：2 个评审，各评 3 维度
        for (long crit : new long[]{1, 2, 3}) {
            service.submitScore(100L, 1L, crit, bd("8"), null);
            service.submitScore(101L, 1L, crit, bd("10"), null);
        }

        List<ContestRank> ranks = service.computeContestRank(1L);
        ContestRank first = ranks.get(0);
        assertEquals(2, first.getJudgeCount());
        // raw sum = (8+8+8) + (10+10+10) = 54, avg = 54 / 2 = 27
        assertEquals(bd("27.00"), first.getAveragePerJudge());
    }

    private BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
