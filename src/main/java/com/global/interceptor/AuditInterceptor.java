package com.global.interceptor;

import com.global.entity.AuditLog;
import com.global.session.audit.AuditLogPersistenceBean;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.EJB;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

@Interceptor
@Audited
public class AuditInterceptor {

    private static final Logger LOGGER =
            Logger.getLogger(AuditInterceptor.class.getName());

    @EJB
    private AuditLogPersistenceBean auditLogPersistenceBean;

    @Resource
    private SessionContext sessionContext;

    @AroundInvoke
    public Object auditInvocation(InvocationContext ctx)
            throws Exception {

        String methodName =
                ctx.getMethod().getName();

        String beanClass =
                ctx.getTarget()
                        .getClass()
                        .getSimpleName();

        String callerPrincipal = "anonymous";

        try {

            if (sessionContext != null
                    && sessionContext.getCallerPrincipal() != null) {

                callerPrincipal =
                        sessionContext
                                .getCallerPrincipal()
                                .getName();
            }

        } catch (Exception e) {

            LOGGER.log(
                    Level.FINE,
                    "[AUDIT] Could not resolve caller principal",
                    e
            );
        }

        long startTime =
                System.currentTimeMillis();

        boolean success = true;
        String errorMessage = null;

        try {


            return ctx.proceed();

        } catch (Exception e) {

            success = false;

            errorMessage =
                    e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage();

            throw e;

        } finally {

            long executionTime =
                    System.currentTimeMillis()
                            - startTime;

            String action =
                    beanClass
                            + "."
                            + methodName;

            persistAuditLog(
                    callerPrincipal,
                    action,
                    beanClass,
                    null,
                    executionTime,
                    success,
                    errorMessage
            );
        }
    }

    private void persistAuditLog(
            String username,
            String action,
            String entityType,
            Long entityId,
            long executionTimeMs,
            boolean success,
            String errorMessage) {

        try {

            AuditLog log =
                    new AuditLog(
                            username,
                            action,
                            entityType,
                            entityId,
                            action
                                    + " executed by "
                                    + username
                    );

            log.setExecutionTimeMs(
                    executionTimeMs
            );

            log.setSuccess(success);

            if (errorMessage != null) {
                log.setErrorMessage(
                        errorMessage
                );
            }


            auditLogPersistenceBean.persist(log);

        } catch (Exception e) {


            LOGGER.log(
                    Level.WARNING,
                    "[AUDIT] Failed to persist audit log for action: "
                            + action,
                    e
            );
        }
    }
}