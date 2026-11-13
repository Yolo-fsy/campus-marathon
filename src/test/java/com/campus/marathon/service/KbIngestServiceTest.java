package com.campus.marathon.service;

import com.campus.marathon.dao.KbChunkDAO;
import com.campus.marathon.dao.impl.KbChunkDAOImpl;
import com.campus.marathon.model.KbChunk;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.util.KbSchema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KbIngestServiceTest {

    private static final KbChunkDAO CHUNK_DAO = new KbChunkDAOImpl();

    @BeforeAll
    static void initSchema() {
        KbSchema.init();
    }

    @Test
    void ingestSplitsDocumentAndPersistsChunks() throws Exception {
        String text = "校园马拉松报名说明：先选择参赛项目，再填写参赛人信息，最后完成 30 元缴费。\n\n"
                + "名额控制：每个项目容量有限，缴费成功后报名状态才会变为报名成功。\n\n"
                + "取消报名：在开赛前取消会原路退款，退款到账后状态变为已取消。\n\n"
                + "审核规则：管理员可以对待审核记录执行通过或驳回，驳回已支付记录会先退款。\n\n"
                + "赛道信息：半程 21.0975 公里，迷你跑 5 公里，欢乐跑 3 公里。\n\n"
                + "联系方式：赛事咨询 sports@campus.edu.cn，紧急情况拨打 120。";

        KbDocument document = new KbIngestService().ingest(
                SessionUser.runner("30990001", "导入测试用户"),
                "赛事手册.md",
                text.getBytes(StandardCharsets.UTF_8));

        assertNotNull(document.getId());
        assertEquals("赛事手册", document.getTitle());
        assertTrue(document.getCharCount() >= text.length() - 10);
        assertTrue(document.getChunkCount() >= 1);

        List<KbChunk> chunks = CHUNK_DAO.listByDocIds(Collections.singletonList(document.getId()));
        assertEquals(document.getChunkCount(), chunks.size());
        assertEquals("赛事手册", chunks.get(0).getDocTitle());
        assertEquals(0, chunks.get(0).getSeq());
        assertTrue(chunks.get(0).getContent().contains("校园马拉松报名说明"));

        for (KbChunk chunk : chunks) {
            assertEquals(chunk.getContent().length(), chunk.getCharCount());
        }
    }

    @Test
    void rejectUnsupportedFileType() {
        KbIngestService service = new KbIngestService();
        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class, () -> service.ingest(
                SessionUser.runner("30990002", "导入测试用户"),
                "报名表.xlsx",
                "不是支持的文档类型".getBytes(StandardCharsets.UTF_8)));
    }
}
