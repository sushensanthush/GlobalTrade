<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.User" %>
<%
    if (session != null && session.getAttribute("currentUser") != null) {
        response.sendRedirect(request.getContextPath() + "/DashboardServlet");
        return;
    }
    String error = request.getParameter("error");
    String logout = request.getParameter("logout");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In | GlobalTrade SCM</title>
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/style.css">

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    <style>
        .input-with-icon {
            position: relative;
        }
        .input-with-icon i {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 14px;
        }
        .input-with-icon input {
            padding-left: 40px;
        }
    </style>
</head>
<body>
<div class="auth-wrapper">
    <div class="auth-card">

        <div class="auth-logo">
            <span class="brand-name">
                GLOBALTRADE
            </span>
            <span class="brand-sub">Supply Chain Management System</span>
        </div>

        <h2 style="text-align: center; margin-bottom: 24px; font-weight: 600; color: var(--text-primary);">
            Sign In to your account
        </h2>

        <% if ("invalid_credentials".equals(error)) { %>
        <div class="alert alert-danger" style="margin-bottom: 20px;">
            <i class="fa-solid fa-circle-exclamation" style="margin-right: 6px;"></i> Invalid username or password. Please try again.
        </div>
        <% } %>

        <% if ("true".equals(logout)) { %>
        <div class="alert alert-success" style="margin-bottom: 20px;">
            <i class="fa-solid fa-circle-check" style="margin-right: 6px;"></i> You have been signed out successfully.
        </div>
        <% } %>

        <form action="<%= request.getContextPath() %>/LoginServlet" method="POST">

            <div class="form-group">
                <label class="form-label" for="username">Username <span class="required">*</span></label>
                <div class="input-with-icon">
                    <i class="fa-solid fa-user"></i>
                    <input type="text" id="username" name="username" class="form-control"
                           placeholder="Enter your username" required autofocus autocomplete="username">
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 24px;">
                <label class="form-label" for="password">Password <span class="required">*</span></label>
                <div class="input-with-icon">
                    <i class="fa-solid fa-lock"></i>
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="Enter your password" required autocomplete="current-password">
                </div>
            </div>

            <button type="submit" class="btn btn-primary" style="width: 100%; padding: 11px; font-size: 15px;">
                <i class="fa-solid fa-right-to-bracket" style="margin-right: 6px;"></i> Sign In
            </button>

        </form>

        <div style="text-align: center; margin-top: 24px;">
            <span style="font-size: 12px; color: var(--text-muted);">
                &copy; 2026 GlobalTrade. Secure Enterprise Portal by susa.
            </span>
        </div>

    </div>
</div>
</body>
</html>