package com.global.security;

import com.global.entity.User;
import com.global.entity.UserRole;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebFilter(urlPatterns = {"/*"})
public class SecurityFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(SecurityFilter.class.getName());


    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/login.jsp", "/LoginServlet", "/error.jsp", "/index.jsp"
    );


    private static final java.util.Map<String, Set<UserRole>> PATH_ROLES =
            java.util.Map.of(
                    "/admin", Set.of(UserRole.SYSTEM_ADMIN),
                    "/customs", Set.of(UserRole.CUSTOMS_AGENT, UserRole.SYSTEM_ADMIN),
                    "/vendor", Set.of(UserRole.VENDOR_REPRESENTATIVE, UserRole.SYSTEM_ADMIN,
                            UserRole.LOGISTICS_COORDINATOR)
            );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String contextPath = request.getContextPath();
        String requestURI = request.getRequestURI();
        String path = requestURI.substring(contextPath.length());


        if (path.startsWith("/css/") || path.startsWith("/js/")
                || path.startsWith("/images/") || path.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }


        for (String publicPath : PUBLIC_PATHS) {
            if (path.equals(publicPath) || path.startsWith(publicPath)) {
                chain.doFilter(request, response);
                return;
            }
        }


        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            LOGGER.log(Level.INFO, "[SECURITY] Unauthenticated access attempt to: {0}", path);
            response.sendRedirect(contextPath + "/login.jsp");
            return;
        }


        for (java.util.Map.Entry<String, Set<UserRole>> entry : PATH_ROLES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                if (!entry.getValue().contains(currentUser.getRole())) {
                    LOGGER.log(Level.WARNING,
                            "[SECURITY] Access denied: user {0} (role {1}) attempted access to {2}",
                            new Object[]{currentUser.getUsername(), currentUser.getRole(), path});
                    response.sendRedirect(contextPath + "/dashboard.jsp?error=access_denied");
                    return;
                }
                break;
            }
        }


        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        chain.doFilter(request, response);
    }
}
