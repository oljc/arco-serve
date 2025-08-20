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

    private static final String SECRET_KEY = "LJCVd05qVXdObU0yT0RaaU5HWTJORGs0TURNek5HTTJZakV6WTJNNE9XVQ==";
    private static final String SECRET_ID = "LJCDEMOYmE1MTU5OTBmNDg5ODlhNTQzMGUwY2YLJCDEMO";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod handlerMethod) {

            Signature sig = AnnotationUtils.find(handlerMethod, Signature.class);

            if (sig != null && !sig.required()) return true;

            long maxAge = sig != null ? sig.maxAge() : 30L;

            try {
                SignUtils.verify(request, SECRET_ID, SECRET_KEY, maxAge);
            } catch (SecurityException e) {
                throw new BusinessException(Code.SIGN_ERROR, e.getMessage());
            }
        }
        return true;
    }
}
