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
 * 知识库文档列表：参赛者看自己的文档，管理员看全部。
 */
public class KbListServlet extends BaseServlet {

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
            request.setAttribute("msg", request.getParameter("msg"));
            request.setAttribute("error", request.getParameter("error"));
            request.setAttribute("activeNav", "ai");
            forward(request, response, "ai-list.jsp");
        } catch (SQLException e) {
            throw new ServletException("加载知识库文档失败", e);
        }
    }
}
