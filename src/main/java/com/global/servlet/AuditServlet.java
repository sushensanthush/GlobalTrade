package com.global.servlet;

import com.global.entity.AuditLog;
import com.global.entity.TimerLog;
import com.global.entity.User;
import com.global.entity.UserRole;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.audit.AuditLogBean;
import com.global.session.auth.AuthenticationBean;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "AuditServlet", urlPatterns = {"/AuditServlet"})
public class AuditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AuditServlet.class.getName());

    @EJB
    private AuditLogBean auditLogBean;

    @EJB
    private AuthenticationBean authBean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (User) session.getAttribute("currentUser");

        try {
            authBean.assertRole(currentUser,
                    UserRole.SYSTEM_ADMIN, UserRole.LOGISTICS_COORDINATOR, UserRole.CUSTOMS_AGENT);

            String hoursParam = request.getParameter("hours");
            int hours = (hoursParam != null && !hoursParam.isBlank()) ? Integer.parseInt(hoursParam) : 24;

            List<AuditLog> auditLogs = auditLogBean.findRecentAuditLogs(hours);
            List<TimerLog> timerLogs = auditLogBean.findRecentTimerLogs(hours);

            request.setAttribute("auditLogs", auditLogs);
            request.setAttribute("timerLogs", timerLogs);
            request.setAttribute("hours", hours);

            request.getRequestDispatcher("/views/audit.jsp").forward(request, response);

        } catch (UnauthorizedAccessException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp?error=access_denied");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[AUDIT] Error loading audit logs", e);
            request.setAttribute("error", e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
