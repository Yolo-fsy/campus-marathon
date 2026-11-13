package com.campus.marathon.service;

import com.campus.marathon.dao.KbChunkDAO;
import com.campus.marathon.dao.impl.KbChunkDAOImpl;
import com.campus.marathon.model.KbChunk;
import com.campus.marathon.util.AiConfig;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 问答编排：检索 top-4 相关切片 → 拼提示词 → 调用大模型流式生成 → 页面附加引用片段。
 */
public class KbChatService {

    private static final int TOP_K = 4;
    private static final String SYSTEM_PROMPT =
            "你是校园马拉松报名系统的知识库助手。规则："
                    + "1) 只能依据下面提供的【资料片段】回答，不要编造事实；"
                    + "2) 如果资料不足以回答，直接说明「资料中没有相关内容」，并建议用户换一种问法或上传相关文档；"
                    + "3) 用简体中文回答，简洁清晰，可在句末用 [编号] 标注引用来源。";

    private final KbChunkDAO chunkDAO;
    private final Bm25Retriever retriever = new Bm25Retriever();
    private final AiConfig config;
    private final LlmClient llmClient;

    public KbChatService() {
        this(AiConfig.load());
    }

    public KbChatService(AiConfig config) {
        this.config = config;
        this.chunkDAO = new KbChunkDAOImpl();
        this.llmClient = new LlmClient(config);
    }

    public List<Bm25Retriever.ScoredChunk> retrieve(List<Long> docIds, String question) throws SQLException {
        List<KbChunk> chunks = chunkDAO.listByDocIds(docIds);
        return retriever.topK(question, chunks, TOP_K);
    }

    public boolean isModelReady() {
        return config.isConfigured();
    }

    public String getModelName() {
        return config.getModel();
    }

    /**
     * 生成回答：有密钥走模型流式输出；没有密钥只回显检索片段，保证流程可演示。
     */
    public void generate(String question, List<Bm25Retriever.ScoredChunk> hits, LlmClient.TokenSink sink)
            throws IOException, InterruptedException {
        if (hits.isEmpty()) {
            sink.onToken("资料库里没有检索到与这个问题相关的内容。可以换一种问法，或先在文档库上传更相关的资料。");
            return;
        }
        if (!config.isConfigured()) {
            sink.onToken(buildRetrievalOnlyAnswer(hits));
            return;
        }
        String context = buildContext(hits);
        String userPrompt = "【资料片段】\n" + context + "\n\n【用户问题】" + question;
        llmClient.streamChat(SYSTEM_PROMPT, userPrompt, sink);
    }

    private String buildContext(List<Bm25Retriever.ScoredChunk> hits) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            KbChunk chunk = hits.get(i).getChunk();
            context.append('[').append(i + 1).append("] 《")
                    .append(chunk.getDocTitle() == null ? "未命名文档" : chunk.getDocTitle())
                    .append("》第 ").append(chunk.getSeq() + 1).append(" 段：")
                    .append(chunk.getContent())
                    .append('\n');
        }
        return context.toString();
    }

    private String buildRetrievalOnlyAnswer(List<Bm25Retriever.ScoredChunk> hits) {
        StringBuilder answer = new StringBuilder();
        answer.append("【检索演示模式】当前未配置模型密钥，先为你展示与问题最相关的资料片段：\n\n");
        for (int i = 0; i < hits.size(); i++) {
            KbChunk chunk = hits.get(i).getChunk();
            answer.append('[').append(i + 1).append("] 《")
                    .append(chunk.getDocTitle() == null ? "未命名文档" : chunk.getDocTitle())
                    .append("》第 ").append(chunk.getSeq() + 1).append(" 段：")
                    .append(chunk.getPreview())
                    .append('\n');
        }
        answer.append("\n在 ai.properties 或环境变量 AI_API_KEY 中配置密钥后，即可得到由大模型生成的完整回答。");
        return answer.toString();
    }
}
