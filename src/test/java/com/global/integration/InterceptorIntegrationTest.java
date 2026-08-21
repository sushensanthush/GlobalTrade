package com.global.integration;

import com.global.exception.VendorValidationException;
import com.global.interceptor.ValidationInterceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("EJB Interceptor Unit Tests")
class InterceptorIntegrationTest {

    @Mock
    private InvocationContext ctx;

    private ValidationInterceptor validationInterceptor;

    @BeforeEach
    void setUp() {
        validationInterceptor = new ValidationInterceptor();
    }

    @Test
    @DisplayName("ValidationInterceptor — null parameter should throw VendorValidationException without calling proceed")
    void validationInterceptor_nullParam_throwsAndNeverProceeds() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{null});

        assertThatThrownBy(() -> validationInterceptor.validateInputs(ctx))
                .isInstanceOf(VendorValidationException.class)
                .hasMessageContaining("Null argument is not permitted");

        verify(ctx, never()).proceed();
    }

    @Test
    @DisplayName("ValidationInterceptor — blank string parameter should throw VendorValidationException")
    void validationInterceptor_blankString_throwsException() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{"   "});

        assertThatThrownBy(() -> validationInterceptor.validateInputs(ctx))
                .isInstanceOf(VendorValidationException.class)
                .hasMessageContaining("Blank string argument is not permitted");

        verify(ctx, never()).proceed();
    }

    @Test
    @DisplayName("ValidationInterceptor — valid parameters should call proceed and return result")
    void validationInterceptor_validParams_callsProceed() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{"valid-value", "another-value"});
        when(ctx.proceed()).thenReturn("SUCCESS");

        Object result = validationInterceptor.validateInputs(ctx);

        assertThat(result).isEqualTo("SUCCESS");
        verify(ctx, times(1)).proceed();
    }

    @Test
    @DisplayName("ValidationInterceptor — empty parameter array should call proceed without validation")
    void validationInterceptor_emptyParams_callsProceed() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{});
        when(ctx.proceed()).thenReturn(null);

        Object result = validationInterceptor.validateInputs(ctx);

        verify(ctx, times(1)).proceed();
    }

    @Test
    @DisplayName("ValidationInterceptor — numeric parameter (non-string) should pass without blank check")
    void validationInterceptor_numericParam_callsProceed() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{42, 3.14});
        when(ctx.proceed()).thenReturn(null);

        assertThatNoException().isThrownBy(() -> validationInterceptor.validateInputs(ctx));
        verify(ctx, times(1)).proceed();
    }

    @Test
    @DisplayName("ValidationInterceptor — mixed valid and null params should fail on null")
    void validationInterceptor_mixedParams_failsOnFirstNull() throws Exception {
        when(ctx.getMethod()).thenReturn(getTestMethod());
        when(ctx.getParameters()).thenReturn(new Object[]{"valid", null, "also-valid"});

        assertThatThrownBy(() -> validationInterceptor.validateInputs(ctx))
                .isInstanceOf(VendorValidationException.class)
                .hasMessageContaining("parameter[1]");

        verify(ctx, never()).proceed();
    }

    private Method getTestMethod() throws NoSuchMethodException {
        return this.getClass().getDeclaredMethod("dummyBusinessMethod", String.class);
    }

    @SuppressWarnings("unused")
    private void dummyBusinessMethod(String param) {

    }
}
