package com.campus.marathon.web;

import com.campus.marathon.dao.KbChunkDAO;
import com.campus.marathon.dao.KbDocumentDAO;
import com.campus.marathon.dao.impl.KbChunkDAOImpl;
import com.campus.marathon.dao.impl.KbDocumentDAOImpl;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * 删除文档：管理员可删任意文档，参赛者只能删自己的。
 */
public class KbDeleteServlet extends BaseServlet {

    private final KbDocumentDAO documentDAO = new KbDocumentDAOImpl();
    private final KbChunkDAO chunkDAO = new KbChunkDAOImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        long id = parseId(request.getParameter("id"));
        if (id <= 0) {
            redirectBack(request, response, "文档编号不合法");
            return;
        }
        try {
            KbDocument document = documentDAO.findById(id);
            if (document == null) {
                redirectBack(request, response, "文档不存在或已被删除");
                return;
            }
            boolean owner = user.getStudentId() != null
                    && user.getStudentId().equals(document.getOwnerStudentId());
            if (!user.isAdmin() && !owner) {
                redirectBack(request, response, "无权删除他人的文档");
                return;
            }
            chunkDAO.deleteByDoc(id);
            documentDAO.delete(id, user.isAdmin() ? null : user.getStudentId());
            redirectBack(request, response, "已删除《" + document.getTitle() + "》");
        } catch (SQLException e) {
            throw new ServletException("删除知识库文档失败", e);
        }
    }

    private void redirectBack(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.sendRedirect(request.getContextPath() + "/ai?msg="
                + URLEncoder.encode(message, StandardCharsets.UTF_8.name()));
    }
}
