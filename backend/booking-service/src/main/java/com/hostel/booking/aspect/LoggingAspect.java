package com.hostel.booking.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Slf4j @Aspect @Component
public class LoggingAspect {

    @Pointcut("execution(* com.hostel.booking.service.*.*(..))")
    public void serviceLayer() {}

    @Before("serviceLayer()")
    public void logBefore(JoinPoint jp) {
        log.info(">> {}.{}({})", jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(), Arrays.toString(jp.getArgs()));
    }

    @AfterReturning(pointcut = "serviceLayer()", returning = "result")
    public void logAfter(JoinPoint jp, Object result) {
        log.info("<< {}.{} returned successfully", jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName());
    }

    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint jp, Exception ex) {
        log.error("!! {}.{} threw: {}", jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(), ex.getMessage(), ex);
    }

    @Around("execution(* com.hostel.booking.controller.*.*(..))")
    public Object logControllerTime(ProceedingJoinPoint jp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = jp.proceed();
        log.info("Controller {}.{} completed in {} ms", jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(), System.currentTimeMillis() - start);
        return result;
    }
}
