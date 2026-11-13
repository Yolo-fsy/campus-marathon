package com.campus.marathon.web;

import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.service.KbIngestService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 上传文档：multipart 解析 → 提取 → 切片 → 入库，然后回到列表页带提示。
 */
public class KbUploadServlet extends BaseServlet {

    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final KbIngestService ingestService = new KbIngestService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            Part part = request.getPart("file");
            if (part == null || part.getSize() == 0) {
                redirectBack(request, response, "error", "请选择要上传的文档");
                return;
            }
            if (part.getSize() > MAX_SIZE_BYTES) {
                redirectBack(request, response, "error", "单个文件不能超过 5MB");
                return;
            }
            KbDocument document = ingestService.ingest(user, part.getSubmittedFileName(), part.getInputStream());
            redirectBack(request, response, "msg",
                    "已导入《" + document.getTitle() + "》，共 " + document.getChunkCount() + " 个切片");
        } catch (IllegalStateException e) {
            redirectBack(request, response, "error", "文件过大，单个文件不能超过 5MB");
        } catch (IOException | java.sql.SQLException e) {
            redirectBack(request, response, "error", e.getMessage() == null ? "导入失败" : e.getMessage());
        }
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String key, String message)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/ai?" + key + "="
                + URLEncoder.encode(message, StandardCharsets.UTF_8.name()));
    }
}
