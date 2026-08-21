package com.global.servlet;

import com.global.entity.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/LogoutServlet"})
public class LogoutServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(LogoutServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);

        String username = "unknown";

        if (session != null) {

            Object currentUser =
                    session.getAttribute("currentUser");

            if (currentUser instanceof User) {
                username = ((User) currentUser).getUsername();
            }


            session.invalidate();

            LOGGER.log(
                    Level.INFO,
                    "[LOGOUT] User session invalidated for: {0}",
                    username
            );
        }


        response.sendRedirect(
                request.getContextPath()
                        + "/login.jsp?logout=true"
        );
    }


    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        doGet(request, response);
    }
}