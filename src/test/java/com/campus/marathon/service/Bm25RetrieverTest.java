package com.campus.marathon.service;

import com.campus.marathon.model.KbChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Bm25RetrieverTest {

    private final Bm25Retriever retriever = new Bm25Retriever();

    private KbChunk chunk(long docId, int seq, String content) {
        KbChunk chunk = new KbChunk();
        chunk.setDocId(docId);
        chunk.setSeq(seq);
        chunk.setContent(content);
        chunk.setDocTitle("测试文档" + docId);
        return chunk;
    }

    @Test
    void tokenizerSplitsChineseIntoBigramsAndKeepsLatinWords() {
        List<String> tokens = Bm25Retriever.tokenize("校园马拉松 2026 报名");
        assertTrue(tokens.contains("校园"));
        assertTrue(tokens.contains("园马"));
        assertTrue(tokens.contains("马拉"));
        assertTrue(tokens.contains("拉松"));
        assertTrue(tokens.contains("2026"));
        assertTrue(tokens.contains("报名"));
    }

    @Test
    void ranksMostRelevantChunkFirst() {
        List<KbChunk> chunks = new ArrayList<>();
        chunks.add(chunk(1, 0, "报名流程：先选择参赛项目，再填写参赛人信息，最后缴费 30 元。"));
        chunks.add(chunk(2, 0, "赛道补给：半程设置三个饮水站，提供香蕉与能量胶。"));
        chunks.add(chunk(3, 0, "报名须知：报名前请确认学号与手机号，报名后不可重复报名。报名成功以缴费为准。"));

        List<Bm25Retriever.ScoredChunk> hits = retriever.topK("报名", chunks, 3);

        // 只有文档 1 与文档 3 命中，文档 3 中「报名」出现频次更高，应排在前面
        assertEquals(2, hits.size());
        assertEquals(3L, hits.get(0).getChunk().getDocId());
        assertEquals(1L, hits.get(1).getChunk().getDocId());
        assertTrue(hits.get(0).getScore() > hits.get(1).getScore());
    }

    @Test
    void unrelatedQueryReturnsNothing() {
        List<KbChunk> chunks = Arrays.asList(
                chunk(1, 0, "报名流程与缴费说明"),
                chunk(2, 0, "赛道补给站位置"));
        assertTrue(retriever.topK("火星探测器轨道", chunks, 4).isEmpty());
    }

    @Test
    void topKLimitsResultSize() {
        List<KbChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            chunks.add(chunk(i + 1, 0, "报名缴费与名额控制说明第 " + i + " 段"));
        }
        assertEquals(2, retriever.topK("报名缴费", chunks, 2).size());
    }
}
