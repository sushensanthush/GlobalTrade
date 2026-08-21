<%@ page contentType="text/html;charset=UTF-8" %>
<%
    if (session != null && session.getAttribute("currentUser") != null) {
        response.sendRedirect(request.getContextPath() + "/DashboardServlet");
    } else {
        response.sendRedirect(request.getContextPath() + "/login.jsp");
    }
%>
