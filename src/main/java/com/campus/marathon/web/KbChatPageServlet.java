package com.campus.marathon.web;

import com.campus.marathon.dao.KbDocumentDAO;
import com.campus.marathon.dao.impl.KbDocumentDAOImpl;
import com.campus.marathon.model.KbDocument;
import com.campus.marathon.model.SessionUser;
import com.campus.marathon.util.KbSchema;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * 问答页：支持 ?docId= 指定单份文档，不指定则在全部可访问文档中检索。
 */
public class KbChatPageServlet extends BaseServlet {

    private final KbDocumentDAO documentDAO = new KbDocumentDAOImpl();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser user = currentUser(request);
        try {
            KbSchema.init();
            List<KbDocument> documents = user.isAdmin()
                    ? documentDAO.listAll()
                    : documentDAO.listByOwner(user.getStudentId());
            request.setAttribute("documents", documents);

            long docId = parseId(request.getParameter("docId"));
            if (docId > 0) {
                KbDocument document = documentDAO.findById(docId);
                if (document == null) {
                    response.sendRedirect(request.getContextPath() + "/ai?error="
                            + java.net.URLEncoder.encode("文档不存在或已被删除", "UTF-8"));
                    return;
                }
                boolean owner = user.getStudentId() != null
                        && user.getStudentId().equals(document.getOwnerStudentId());
                if (!user.isAdmin() && !owner) {
                    response.sendRedirect(request.getContextPath() + "/ai?error="
                            + java.net.URLEncoder.encode("无权访问该文档", "UTF-8"));
                    return;
                }
                request.setAttribute("doc", document);
                request.setAttribute("docId", docId);
            }
            request.setAttribute("activeNav", "ai");
            forward(request, response, "ai-chat.jsp");
        } catch (SQLException e) {
            throw new ServletException("加载问答页失败", e);
        }
    }
}
