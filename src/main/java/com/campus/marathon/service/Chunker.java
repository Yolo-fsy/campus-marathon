package com.campus.marathon.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切片：500 字窗口 + 100 字重叠，尽量在段落或句号边界处断开，避免切断完整句子。
 */
public class Chunker {

    static final int WINDOW_SIZE = 500;
    static final int OVERLAP_SIZE = 100;
    private static final int BOUNDARY_LOOKBACK = 120;

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return chunks;
        }
        if (normalized.length() <= WINDOW_SIZE) {
            chunks.add(normalized);
            return chunks;
        }

        int length = normalized.length();
        int start = 0;
        while (start < length) {
            int end = Math.min(start + WINDOW_SIZE, length);
            if (end < length) {
                int boundary = findBoundary(normalized, end);
                if (boundary > start + OVERLAP_SIZE) {
                    end = boundary;
                }
            }
            String piece = normalized.substring(start, end).trim();
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
            if (end >= length) {
                break;
            }
            int nextStart = end - OVERLAP_SIZE;
            start = nextStart > start ? nextStart : end;
        }
        return chunks;
    }

    /**
     * 从窗口右边界向左找最近的段落/句末位置，返回切分点（该位置本身保留在前一片）。
     */
    private int findBoundary(String text, int from) {
        int lower = Math.max(from - BOUNDARY_LOOKBACK, 1);
        for (int i = from; i > lower; i--) {
            char previous = text.charAt(i - 1);
            if (previous == '\n' || previous == '。' || previous == '！' || previous == '？'
                    || previous == '；' || previous == ';' || previous == '.' || previous == '!'
                    || previous == '?') {
                return i;
            }
        }
        return from;
    }

    static String normalize(String text) {
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\t\u000B\u000C]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
