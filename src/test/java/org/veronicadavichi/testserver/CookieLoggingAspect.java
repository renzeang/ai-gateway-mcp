package org.veronicadavichi.testserver;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collections;

/**
 * AOP 切面 - 记录所有 REST 接口请求的 Cookie
 */
@Aspect
@Component
public class CookieLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(CookieLoggingAspect.class);

    /**
     * 环绕通知 - 拦截所有 @RestController 中的方法
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logCookies(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String method = request.getMethod();
            String path = request.getRequestURI();

            // 获取所有 Cookie
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();

            if (cookies == null || cookies.length == 0) {
                logger.info("[REST Request] {} {} - No cookies", method, path);
            } else {
                logger.info("[REST Request] {} {} - Cookies: {}",
                    method,
                    path,
                    Arrays.toString(cookies)
                );
            }
        }

        return joinPoint.proceed();
    }
}
