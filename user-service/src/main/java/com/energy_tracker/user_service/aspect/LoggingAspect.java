package com.energy_tracker.user_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.energy_tracker.user_service.service.*.*(..))")
    public void logServiceMethods() {}

    @Before("logServiceMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Called service method: {} with a args: {}",
                joinPoint.getSignature().getName(), joinPoint.getArgs());
    }

    @AfterReturning(pointcut = "logServiceMethods()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Service method completed: {} with result: {}",
                joinPoint.getSignature().getName(), result);
    }
}
