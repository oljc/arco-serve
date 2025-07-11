package io.github.oljc.arcoserve.shared.web;

import io.github.oljc.arcoserve.shared.annotation.Signature;
import io.github.oljc.arcoserve.shared.exception.BusinessException;
import io.github.oljc.arcoserve.shared.exception.Code;
import io.github.oljc.arcoserve.shared.util.AnnotationUtils;
import io.github.oljc.arcoserve.shared.util.SignUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SignInterceptor implements HandlerInterceptor {

    private static final String HEADER_DATE = "X-Date";
    private static final String HEADER_FP = "X-Fingerprint";
    private static final String SECRET_KEY = "LJCVd05qVXdObU0yT0RaaU5HWTJORGs0TURNek5HTTJZakV6WTJNNE9XVQ==";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {

            Signature signature = AnnotationUtils.find(handlerMethod, Signature.class);
            if (signature != null && signature.required()) {
                checkSignature(request, signature);
            }
        }
        return true;
    }

    private void checkSignature(HttpServletRequest request, Signature signature) {
        String xDate = request.getHeader(HEADER_DATE);
        String fingerprint = request.getHeader(HEADER_FP);

        if (xDate == null || fingerprint == null) {
            throw new BusinessException(Code.SIGN_MISSING);
        }

        if (SignUtils.isExpired(xDate, signature.maxAge())) {
            throw new BusinessException(Code.SIGN_EXPIRED);
        }

        try {
            SignUtils.verify(request, SECRET_KEY);
        } catch (Exception e) {
            throw new BusinessException(Code.SIGN_ERROR);
        }
    }
}
