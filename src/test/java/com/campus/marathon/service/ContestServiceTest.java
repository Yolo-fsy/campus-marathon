package com.campus.marathon.service;

import com.campus.marathon.dao.CriterionDAO;
import com.campus.marathon.dao.ContestDAO;
import com.campus.marathon.dao.ContestantDAO;
import com.campus.marathon.model.Contest;
import com.campus.marathon.model.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ContestService 单元测试：权重校验、状态流转、评审阶段禁止改维度。
 * 使用内存假 DAO。
 */
class ContestServiceTest {

    /** 内存假 ContestantDAO（空实现，本类主要用不到） */
    static class InMemoryContestantDAO implements ContestantDAO {
        @Override public long create(com.campus.marathon.model.Contestant contestant) { return 0; }
        @Override public com.campus.marathon.model.Contestant findById(long id) { return null; }
        @Override public List<com.campus.marathon.model.Contestant> listByContest(long contestId) { return new ArrayList<>(); }
        @Override public boolean delete(long id, long contestId) { return false; }
        @Override public boolean update(com.campus.marathon.model.Contestant contestant) { return false; }
    }

    /** 内存假 ContestDAO */
    static class InMemoryContestDAO implements ContestDAO {
        private final Map<Long, Contest> store = new HashMap<>();
        private long nextId = 1;

        void seed(Contest c) { store.put(c.getId(), c); }

        @Override
        public long create(Contest contest) {
            long id = nextId++;
            contest.setId(id);
            store.put(id, contest);
            return id;
        }
        @Override
        public Contest findById(long id) { return store.get(id); }
        @Override
        public List<Contest> listAll() { return new ArrayList<>(store.values()); }
        @Override
        public List<Contest> listByStatus(String status) {
            List<Contest> out = new ArrayList<>();
            for (Contest c : store.values()) {
                if (status.equals(c.getStatus())) out.add(c);
            }
            return out;
        }
        @Override
        public boolean updateStatus(long id, String status) {
            Contest c = store.get(id);
            if (c == null) return false;
            c.setStatus(status);
            return true;
        }
        @Override
        public boolean update(Contest contest) {
            Contest c = store.get(contest.getId());
            if (c == null) return false;
            c.setName(contest.getName());
            c.setDescription(contest.getDescription());
            return true;
        }
        @Override
        public boolean delete(long id) { return store.remove(id) != null; }
    }

    /** 内存假 CriterionDAO */
    static class InMemoryCriterionDAO implements CriterionDAO {
        private final Map<Long, Criterion> store = new HashMap<>();
        private long nextId = 1;

        void seed(Criterion c) { store.put(c.getId(), c); nextId = Math.max(nextId, c.getId() + 1); }

        @Override
        public long create(Criterion criterion) {
            long id = nextId++;
            criterion.setId(id);
            store.put(id, criterion);
            return id;
        }
        @Override
        public List<Criterion> listByContest(long contestId) {
            List<Criterion> out = new ArrayList<>();
            for (Criterion c : store.values()) {
                if (c.getContestId() == contestId) out.add(c);
            }
            return out;
        }
        @Override
        public boolean updateWeight(long id, long contestId, int weight) {
            Criterion c = store.get(id);
            if (c == null || c.getContestId() != contestId) return false;
            c.setWeight(weight);
            return true;
        }
        @Override
        public boolean update(Criterion criterion) {
            Criterion c = store.get(criterion.getId());
            if (c == null || c.getContestId() != criterion.getContestId()) return false;
            c.setName(criterion.getName());
            c.setWeight(criterion.getWeight());
            c.setDescription(criterion.getDescription());
            return true;
        }
        @Override
        public boolean delete(long id, long contestId) {
            Criterion c = store.get(id);
            if (c == null || c.getContestId() != contestId) return false;
            store.remove(id);
            return true;
        }
        @Override
        public boolean deleteByContest(long contestId) {
            return store.entrySet().removeIf(e -> e.getValue().getContestId() == contestId);
        }
    }

    private InMemoryContestDAO contestDAO;
    private InMemoryCriterionDAO criterionDAO;
    private ContestService service;

    @BeforeEach
    void setUp() {
        contestDAO = new InMemoryContestDAO();
        criterionDAO = new InMemoryCriterionDAO();
        service = new ContestService(contestDAO, criterionDAO, new InMemoryContestantDAO());
    }

    @Test
    @DisplayName("新建竞赛默认 DRAFT 状态")
    void createContestDefaultsToDraft() throws SQLException {
        Contest c = service.create("程序设计大赛", "描述");
        assertEquals(1L, c.getId());
        assertEquals("程序设计大赛", c.getName());
        assertEquals(Contest.STATUS_DRAFT, c.getStatus());
    }

    @Test
    @DisplayName("空名称抛异常")
    void createContestRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> service.create("", "描述"));
        assertThrows(IllegalArgumentException.class, () -> service.create(null, "描述"));
    }

    @Test
    @DisplayName("启动评审成功：DRAFT → JUDGING，维度权重和 100")
    void startJudgingWorksWhenWeightsSum100() throws SQLException {
        Contest c = new Contest();
        c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        Criterion a = new Criterion(); a.setId(10); a.setContestId(1); a.setWeight(30);
        Criterion b = new Criterion(); b.setId(11); b.setContestId(1); b.setWeight(40);
        Criterion c2 = new Criterion(); c2.setId(12); c2.setContestId(1); c2.setWeight(30);
        criterionDAO.seed(a); criterionDAO.seed(b); criterionDAO.seed(c2);

        service.startJudging(1L);
        assertEquals(Contest.STATUS_JUDGING, contestDAO.findById(1L).getStatus());
    }

    @Test
    @DisplayName("启动评审失败：权重和不为 100")
    void startJudgingRejectsBadWeightSum() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        Criterion a = new Criterion(); a.setId(10); a.setContestId(1); a.setWeight(30);
        Criterion b = new Criterion(); b.setId(11); b.setContestId(1); b.setWeight(40);
        criterionDAO.seed(a); criterionDAO.seed(b); // 权重和 = 70

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.startJudging(1L));
        assertTrue(ex.getMessage().contains("100"), "应提示权重和必须为 100");
    }

    @Test
    @DisplayName("启动评审失败：没有维度")
    void startJudgingRejectsNoCriteria() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.startJudging(1L));
        assertTrue(ex.getMessage().contains("至少需要一个评分维度"));
    }

    @Test
    @DisplayName("状态流转：JUDGING → FINISHED")
    void finishContestTransitionsJudgingToFinished() throws SQLException {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_JUDGING);
        contestDAO.seed(c);

        service.finishContest(1L);
        assertEquals(Contest.STATUS_FINISHED, contestDAO.findById(1L).getStatus());
    }

    @Test
    @DisplayName("状态流转：DRAFT 不能直接 FINISHED")
    void cannotSkipToFinished() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        assertThrows(IllegalStateException.class, () -> service.finishContest(1L));
    }

    @Test
    @DisplayName("回退草稿：JUDGING → DRAFT")
    void revertToDraftWorks() throws SQLException {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_JUDGING);
        contestDAO.seed(c);

        service.revertToDraft(1L);
        assertEquals(Contest.STATUS_DRAFT, contestDAO.findById(1L).getStatus());
    }

    @Test
    @DisplayName("评审阶段禁止新增维度")
    void addCriterionForbiddenInJudging() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_JUDGING);
        contestDAO.seed(c);

        assertThrows(IllegalStateException.class, () -> service.addCriterion(1L, "新维度", 30, "说明"));
    }

    @Test
    @DisplayName("评审阶段禁止修改维度权重")
    void updateCriterionForbiddenInJudging() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_JUDGING);
        contestDAO.seed(c);
        Criterion cr = new Criterion(); cr.setId(10); cr.setContestId(1); cr.setName("技术难度"); cr.setWeight(30);
        criterionDAO.seed(cr);

        assertThrows(IllegalStateException.class, () -> service.updateCriterion(1L, 10L, "技术难度", 50, "说明"));
    }

    @Test
    @DisplayName("评审阶段禁止删除维度")
    void deleteCriterionForbiddenInJudging() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_JUDGING);
        contestDAO.seed(c);
        Criterion cr = new Criterion(); cr.setId(10); cr.setContestId(1); cr.setName("技术难度"); cr.setWeight(30);
        criterionDAO.seed(cr);

        assertThrows(IllegalStateException.class, () -> service.deleteCriterion(1L, 10L));
    }

    @Test
    @DisplayName("已结束竞赛也禁止改维度")
    void allCriterionChangesForbiddenWhenFinished() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_FINISHED);
        contestDAO.seed(c);

        assertThrows(IllegalStateException.class, () -> service.addCriterion(1L, "X", 10, null));
    }

    @Test
    @DisplayName("草稿阶段维度操作全部允许")
    void allCriterionChangesAllowedInDraft() throws SQLException {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        long critId = service.addCriterion(1L, "技术难度", 30, "说明");
        assertTrue(critId > 0);

        service.updateCriterion(1L, critId, "技术难度", 40, "修改后的说明");
        Criterion found = criterionDAO.listByContest(1L).get(0);
        assertEquals(40, found.getWeight());

        service.deleteCriterion(1L, critId);
        assertTrue(criterionDAO.listByContest(1L).isEmpty());
    }

    @Test
    @DisplayName("权重范围校验：0-100 之间")
    void weightRangeValidation() {
        Contest c = new Contest(); c.setId(1); c.setStatus(Contest.STATUS_DRAFT);
        contestDAO.seed(c);

        assertThrows(IllegalArgumentException.class, () -> service.addCriterion(1L, "X", -1, null));
        assertThrows(IllegalArgumentException.class, () -> service.addCriterion(1L, "X", 101, null));

        assertDoesNotThrow(() -> service.addCriterion(1L, "X", 0, null));
        assertDoesNotThrow(() -> service.addCriterion(1L, "Y", 100, null));
    }
}
