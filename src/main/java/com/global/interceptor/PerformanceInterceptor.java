package com.global.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Interceptor
@PerformanceMonitored
public class PerformanceInterceptor {

    private static final Logger LOGGER = Logger.getLogger(PerformanceInterceptor.class.getName());

    private static final long WARNING_THRESHOLD_MS = 500L;
    private static final long SEVERE_THRESHOLD_MS = 2000L;

    @AroundInvoke
    public Object monitorPerformance(InvocationContext ctx) throws Exception {
        String methodSignature = ctx.getTarget().getClass().getSimpleName()
                + "." + ctx.getMethod().getName();
        long startTime = System.currentTimeMillis();

        try {
            return ctx.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            logPerformanceMetric(methodSignature, elapsed);
        }
    }

    private void logPerformanceMetric(String methodSignature, long elapsedMs) {
        if (elapsedMs >= SEVERE_THRESHOLD_MS) {
            LOGGER.log(Level.SEVERE,
                    "[PERFORMANCE-CRITICAL] {0} took {1}ms — exceeds 2000ms SLA threshold. " +
                    "Investigate for supply chain bottleneck.",
                    new Object[]{methodSignature, elapsedMs});
        } else if (elapsedMs >= WARNING_THRESHOLD_MS) {
            LOGGER.log(Level.WARNING,
                    "[PERFORMANCE-WARNING] {0} took {1}ms — exceeds 500ms advisory threshold.",
                    new Object[]{methodSignature, elapsedMs});
        } else {
            LOGGER.log(Level.FINE,
                    "[PERFORMANCE-OK] {0} completed in {1}ms.",
                    new Object[]{methodSignature, elapsedMs});
        }
    }
}
