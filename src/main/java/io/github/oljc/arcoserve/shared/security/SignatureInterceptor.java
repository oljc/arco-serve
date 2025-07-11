package io.github.oljc.arcoserve.shared.security;

import io.github.oljc.arcoserve.shared.annotation.Signature;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.ResultCode;
import io.github.oljc.arcoserve.shared.util.SignUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

@Component
public class SignatureInterceptor implements HandlerInterceptor {

    private static final String HEADER_DATE = "X-Date";
    private static final String HEADER_FP = "X-Fingerprint";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Signature signature = resolveSignatureAnnotation(handlerMethod);
            if (signature != null && signature.required()) {
                validateSignature(request, signature);
            }
        }
        return true;
    }

    private void validateSignature(HttpServletRequest request, Signature signature) {
        String xDate = request.getHeader(HEADER_DATE);
        String fingerprint = request.getHeader(HEADER_FP);

        if (xDate == null || fingerprint == null) {
            throw new BusinessException(ResultCode.SIGNATURE_MISSING);
        }

        if (SignUtils.isExpired(xDate, signature.maxAge())) {
            throw new BusinessException(ResultCode.SIGNATURE_EXPIRED);
        }

        try {
            SignUtils.verify(request);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SIGNATURE_VERIFICATION_FAILED, "签名校验失败: " + e.getMessage());
        }
    }

    /**
     * 优先找方法上的注解，找不到再找类上的
     */
    private Signature resolveSignatureAnnotation(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        Class<?> beanType = handlerMethod.getBeanType();

        Signature methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, Signature.class);
        if (methodAnnotation != null) return methodAnnotation;

        return AnnotatedElementUtils.findMergedAnnotation(beanType, Signature.class);
    }
}
