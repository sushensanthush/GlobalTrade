<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="com.global.entity.*" %>
<%@ page import="java.util.*" %>
<%
    if (session == null || session.getAttribute("currentUser") == null) {
        response.sendRedirect(request.getContextPath() + "/login.jsp"); return;
    }
    User currentUser = (User) session.getAttribute("currentUser");
    String ctx = request.getContextPath();

    @SuppressWarnings("unchecked")
    List<AuditLog> auditLogs = (List<AuditLog>) request.getAttribute("auditLogs");
    @SuppressWarnings("unchecked")
    List<TimerLog> timerLogs = (List<TimerLog>) request.getAttribute("timerLogs");
    if (auditLogs == null) auditLogs = new ArrayList<>();
    if (timerLogs == null) timerLogs = new ArrayList<>();
    Integer hours = (Integer) request.getAttribute("hours");
    if (hours == null) hours = 24;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Audit Trail | GlobalTrade</title>
    <link rel="stylesheet" href="<%= ctx %>/css/style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
</head>
<body>
<div class="app-shell">
    <%@ include file="/WEB-INF/nav-include.jsp" %>
    <div class="main-content">
        <div class="topbar">
            <span class="topbar-title">Audit Trail &amp; Timer Logs</span>
            <div class="topbar-actions">
                <form action="<%= ctx %>/AuditServlet" method="GET" style="display:flex; gap:8px; align-items:center;">
                    <label style="font-size:13px; color:var(--text-secondary);">Last</label>
                    <select name="hours" class="form-control" style="width:100px; padding:6px 10px;">
                        <option value="1"  <%= hours == 1  ? "selected" : "" %>>1 hour</option>
                        <option value="6"  <%= hours == 6  ? "selected" : "" %>>6 hours</option>
                        <option value="24" <%= hours == 24 ? "selected" : "" %>>24 hours</option>
                        <option value="72" <%= hours == 72 ? "selected" : "" %>>72 hours</option>
                    </select>
                    <button type="submit" class="btn btn-outline btn-sm">Filter</button>
                </form>
            </div>
        </div>
        <div class="page-body">


            <div class="card mb-6">
                <div class="card-header">
                    <span class="card-title">EJB Timer Service Executions (<%= timerLogs.size() %>)</span>
                    <span class="badge badge-info">Last <%= hours %> hour(s)</span>
                </div>
                <div class="card-body p-0">
                    <% if (timerLogs.isEmpty()) { %>
                    <div class="empty-state">
                        <p>No timer executions recorded in the last <%= hours %> hour(s).</p>
                        <p style="font-size:12px; margin-top:4px;">Timers run on schedule — check back shortly.</p>
                    </div>
                    <% } else { %>
                    <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Timer Type</th>
                            <th>Description</th>
                            <th>Records Processed</th>
                            <th>Exec Time (ms)</th>
                            <th>Result</th>
                            <th>Executed At</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (TimerLog tl : timerLogs) { %>
                        <tr>
                            <td class="fw-600 mono" style="font-size:12px;"><%= tl.getTimerType() %></td>
                            <td class="text-muted" style="font-size:12px;"><%= tl.getDescription() != null ? tl.getDescription() : "—" %></td>
                            <td class="text-center"><%= tl.getRecordsProcessed() %></td>
                            <td class="mono text-muted"><%= tl.getExecutionTimeMs() != null ? tl.getExecutionTimeMs() : "—" %></td>
                            <td>
                                <span class="badge <%= tl.isSuccess() ? "badge-success" : "badge-danger" %>">
                                    <%= tl.isSuccess() ? "SUCCESS" : "FAILED" %>
                                </span>
                            </td>
                            <td class="text-muted" style="font-size:12px;">
                                <%= tl.getExecutedAt() != null ? tl.getExecutedAt().toString().replace("T"," ").substring(0,19) : "—" %>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    </div>
                    <% } %>
                </div>
            </div>


            <div class="card">
                <div class="card-header">
                    <span class="card-title">Business Method Audit Log (<%= auditLogs.size() %>)</span>
                    <span class="badge badge-info">Last <%= hours %> hour(s)</span>
                </div>
                <div class="card-body p-0">
                    <% if (auditLogs.isEmpty()) { %>
                    <div class="empty-state"><p>No audit events in the selected time window.</p></div>
                    <% } else { %>
                    <div class="table-container">
                    <table>
                        <thead>
                        <tr>
                            <th>Action / Method</th>
                            <th>Entity Type</th>
                            <th>User</th>
                            <th>Exec Time (ms)</th>
                            <th>Result</th>
                            <th>Error</th>
                            <th>Timestamp</th>
                        </tr>
                        </thead>
                        <tbody>
                        <% for (AuditLog log : auditLogs) { %>
                        <tr>
                            <td class="mono fw-600" style="font-size:12px;"><%= log.getAction() %></td>
                            <td class="text-muted" style="font-size:12px;"><%= log.getEntityType() != null ? log.getEntityType() : "—" %></td>
                            <td style="font-size:12px;"><%= log.getUsername() != null ? log.getUsername() : "anonymous" %></td>
                            <td class="mono text-muted"><%= log.getExecutionTimeMs() != null ? log.getExecutionTimeMs() : "—" %></td>
                            <td>
                                <span class="badge <%= log.isSuccess() ? "badge-success" : "badge-danger" %>">
                                    <%= log.isSuccess() ? "OK" : "FAIL" %>
                                </span>
                            </td>
                            <td class="text-danger" style="font-size:11px; max-width:200px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
                                <%= log.getErrorMessage() != null ? log.getErrorMessage() : "" %>
                            </td>
                            <td class="text-muted" style="font-size:12px;">
                                <%= log.getTimestamp() != null ? log.getTimestamp().toString().replace("T"," ").substring(0,19) : "—" %>
                            </td>
                        </tr>
                        <% } %>
                        </tbody>
                    </table>
                    </div>
                    <% } %>
                </div>
                <div class="card-footer">
                    <span class="text-muted" style="font-size:12px;">
                        Audit logs are written by the <strong>AuditInterceptor</strong> EJB interceptor on every
                        business method invocation. Timer execution logs are written by the
                        <strong>SupplyChainTimerService</strong> singleton after each @Schedule callback completes.
                        Both contribute to the customs compliance audit trail required under international trade regulations.
                    </span>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
