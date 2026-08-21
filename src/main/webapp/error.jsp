<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<%
    String ctx = request.getContextPath();
    String errorMsg = (String) request.getAttribute("error");
    if (errorMsg == null) errorMsg = "An unexpected error occurred. Please try again.";
    Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
</head>
<body>
<div style="min-height:100vh; display:flex; align-items:center; justify-content:center; background:var(--bg); padding:24px;">
    <div style="max-width:520px; width:100%; text-align:center;">
        <div style="font-size:64px; font-weight:800; color:var(--border); line-height:1; margin-bottom:16px;">
            <%= statusCode != null ? statusCode : "ERR" %>
        </div>
        <h2 style="font-size:22px; font-weight:700; color:var(--text-primary); margin-bottom:12px;">
            Operation Failed
        </h2>
        <div class="alert alert-danger" style="text-align:left; margin-bottom:24px;">
            <%= errorMsg %>
        </div>
        <div style="display:flex; gap:12px; justify-content:center;">
            <a href="javascript:history.back()" class="btn btn-outline">Go Back</a>
            <a href="<%= ctx %>/DashboardServlet" class="btn btn-primary">Return to Dashboard</a>
        </div>
    </div>
</div>
</body>
</html>
