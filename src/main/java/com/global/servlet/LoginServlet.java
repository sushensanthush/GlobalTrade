package com.global.servlet;

import com.global.entity.User;
import com.global.exception.UnauthorizedAccessException;
import com.global.session.auth.AuthenticationBean;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(LoginServlet.class.getName());

    @EJB
    private AuthenticationBean authBean;

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");


        if (username == null || username.trim().isEmpty()
                || password == null || password.isEmpty()) {

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=invalid_credentials"
            );
            return;
        }

        try {


            User user = authBean.login(username.trim(), password);


            HttpSession oldSession = request.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }


            HttpSession session = request.getSession(true);


            session.setAttribute("currentUser", user);


            if (user.getRole() != null) {
                session.setAttribute(
                        "userRole",
                        user.getRole().name()
                );
            }


            session.setMaxInactiveInterval(30 * 60);

            LOGGER.log(
                    Level.INFO,
                    "[LOGIN] User authenticated: {0} ({1})",
                    new Object[]{
                            user.getUsername(),
                            user.getRole()
                    }
            );


            response.sendRedirect(
                    request.getContextPath()
                            + "/DashboardServlet"
            );
            return;

        } catch (UnauthorizedAccessException e) {

            LOGGER.log(
                    Level.WARNING,
                    "[LOGIN] Failed login attempt for username: {0}",
                    username
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=invalid_credentials"
            );
            return;

        } catch (Exception e) {


            LOGGER.log(
                    Level.SEVERE,
                    "[LOGIN] Unexpected error while authenticating user: "
                            + username,
                    e
            );

            response.sendRedirect(
                    request.getContextPath()
                            + "/login.jsp?error=server_error"
            );
            return;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {


        response.sendRedirect(
                request.getContextPath()
                        + "/login.jsp"
        );
    }
}