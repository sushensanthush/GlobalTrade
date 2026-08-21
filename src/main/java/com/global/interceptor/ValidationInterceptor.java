package com.global.interceptor;

import com.global.exception.VendorValidationException;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Interceptor
@ValidatedInput
public class ValidationInterceptor {

    private static final Logger LOGGER = Logger.getLogger(ValidationInterceptor.class.getName());

    @AroundInvoke
    public Object validateInputs(InvocationContext ctx) throws Exception {
        String methodName = ctx.getMethod().getName();
        Object[] params = ctx.getParameters();

        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                Object param = params[i];
                if (param == null) {
                    LOGGER.log(Level.WARNING,
                            "[VALIDATION] Null parameter at index {0} in method {1}",
                            new Object[]{i, methodName});
                    throw new VendorValidationException("parameter[" + i + "]",
                            "Null argument is not permitted in supply chain operations");
                }
                if (param instanceof String str && str.isBlank()) {
                    LOGGER.log(Level.WARNING,
                            "[VALIDATION] Blank string parameter at index {0} in method {1}",
                            new Object[]{i, methodName});
                    throw new VendorValidationException("parameter[" + i + "]",
                            "Blank string argument is not permitted in supply chain operations");
                }
            }
        }

        LOGGER.log(Level.FINE, "[VALIDATION] All parameters valid for method: {0}", methodName);
        return ctx.proceed();
    }
}
