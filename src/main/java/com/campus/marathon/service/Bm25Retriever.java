package com.campus.marathon.service;

import com.campus.marathon.model.KbChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 中文 bigram 分词 + BM25 相关度检索（纯 Java，无外部依赖）。
 */
public class Bm25Retriever {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    /**
     * 带相关度分数的切片。
     */
    public static class ScoredChunk {

        private final KbChunk chunk;
        private final double score;

        public ScoredChunk(KbChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }

        public KbChunk getChunk() {
            return chunk;
        }

        public double getScore() {
            return score;
        }
    }

    public List<ScoredChunk> topK(String query, List<KbChunk> chunks, int k) {
        if (query == null || chunks == null || chunks.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }
        List<String> queryTerms = new ArrayList<>(new HashSet<>(tokenize(query)));
        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        int totalChunks = chunks.size();
        List<List<String>> documentTerms = new ArrayList<>(totalChunks);
        Map<String, Integer> documentFrequency = new HashMap<>();
        int totalLength = 0;
        for (KbChunk chunk : chunks) {
            List<String> terms = tokenize(chunk.getContent());
            documentTerms.add(terms);
            totalLength += terms.size();
            for (String term : new HashSet<>(terms)) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        double averageLength = totalLength == 0 ? 1.0 : (double) totalLength / totalChunks;

        List<ScoredChunk> scored = new ArrayList<>();
        for (int i = 0; i < totalChunks; i++) {
            List<String> terms = documentTerms.get(i);
            double score = 0.0;
            for (String term : queryTerms) {
                int frequency = Collections.frequency(terms, term);
                if (frequency == 0) {
                    continue;
                }
                int df = documentFrequency.getOrDefault(term, 0);
                double idf = Math.log(1.0 + (totalChunks - df + 0.5) / (df + 0.5));
                double denominator = frequency + K1 * (1 - B + B * terms.size() / averageLength);
                score += idf * frequency * (K1 + 1) / denominator;
            }
            if (score > 0) {
                scored.add(new ScoredChunk(chunks.get(i), score));
            }
        }
        scored.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());
        if (scored.size() > k) {
            return new ArrayList<>(scored.subList(0, k));
        }
        return scored;
    }

    /**
     * 中文按连续字串生成 bigram（单字串保留原字）；英文与数字按连续字母数字串整体成词。
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        StringBuilder latin = new StringBuilder();
        StringBuilder cjk = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                flushLatin(latin, tokens);
                cjk.append(c);
            } else if (Character.isLetterOrDigit(c)) {
                flushCjk(cjk, tokens);
                latin.append(c);
            } else {
                flushLatin(latin, tokens);
                flushCjk(cjk, tokens);
            }
        }
        flushLatin(latin, tokens);
        flushCjk(cjk, tokens);
        return tokens;
    }

    private static void flushLatin(StringBuilder latin, List<String> tokens) {
        if (latin.length() > 0) {
            tokens.add(latin.toString().toLowerCase());
            latin.setLength(0);
        }
    }

    private static void flushCjk(StringBuilder cjk, List<String> tokens) {
        if (cjk.length() == 1) {
            tokens.add(cjk.toString());
        } else if (cjk.length() > 1) {
            for (int i = 0; i + 1 < cjk.length(); i++) {
                tokens.add(cjk.substring(i, i + 2));
            }
        }
        cjk.setLength(0);
    }

    private static boolean isCjk(char c) {
        return (c >= '\u4E00' && c <= '\u9FFF') || (c >= '\u3400' && c <= '\u4DBF');
    }
}
