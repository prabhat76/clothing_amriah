package com.clothing.ai.common.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        long start = (long) req.getAttribute("startTime");
        long ms = System.currentTimeMillis() - start;
        if (ms > 500) log.warn("SLOW {} {} -> {} ({}ms)", req.getMethod(), req.getRequestURI(), res.getStatus(), ms);
        else if (log.isDebugEnabled()) log.debug("{} {} -> {} ({}ms)", req.getMethod(), req.getRequestURI(), res.getStatus(), ms);
    }
}
