package com.campus.marathon.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkerTest {

    private final Chunker chunker = new Chunker();

    @Test
    void nullAndBlankProduceNoChunks() {
        assertTrue(chunker.split(null).isEmpty());
        assertTrue(chunker.split("   \n\n  ").isEmpty());
    }

    @Test
    void shortTextStaysSingleChunk() {
        List<String> chunks = chunker.split("报名流程：选择项目 → 填写信息 → 缴费 30 元。");
        assertEquals(1, chunks.size());
        assertEquals("报名流程：选择项目 → 填写信息 → 缴费 30 元。", chunks.get(0));
    }

    @Test
    void longTextIsSplitWithOverlapAndWindowLimit() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            text.append("第").append(i).append("段：校园马拉松报名需要核验学号与手机号，缴费成功后报名状态变为报名成功。");
        }
        List<String> chunks = chunker.split(text.toString());

        assertTrue(chunks.size() > 1, "超长文本必须被切成多片");
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= Chunker.WINDOW_SIZE, "切片长度不能超过窗口: " + chunk.length());
        }
        // 相邻切片存在重叠：后一片开头应能在前一片结尾附近找到
        String first = chunks.get(0);
        String second = chunks.get(1);
        boolean overlapped = false;
        for (int length = Math.min(first.length(), second.length()); length >= 20; length--) {
            if (first.endsWith(second.substring(0, length))) {
                overlapped = true;
                break;
            }
        }
        assertTrue(overlapped, "相邻切片应存在重叠窗口");
    }

    @Test
    void prefersSentenceBoundary() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            text.append("这是第").append(i).append("句用于测试边界切分的说明文字，长度刚好接近窗口。");
        }
        List<String> chunks = chunker.split(text.toString());
        for (String chunk : chunks) {
            assertTrue(chunk.endsWith("。"), "尽量在句号处断开，实际结尾: " + chunk.substring(Math.max(0, chunk.length() - 12)));
        }
    }
}
