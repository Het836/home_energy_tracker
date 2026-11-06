package com.energy_tracker.device_service.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Slf4j
@Component
public class ExecutionTimeAspect {

    @Pointcut("execution(* com.energy_tracker.device_service.service.*.*(..))")
    public void logServiceMethods() {}

    @Around("logServiceMethods()")
    public Object measureExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long end = System.nanoTime();
            long elapsedNs = end - start;
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsedNs);
            String signature = pjp.getSignature().toShortString();
            log.info("Controller method {} in {} ms", signature, elapsedMs);
        }
    }
}
