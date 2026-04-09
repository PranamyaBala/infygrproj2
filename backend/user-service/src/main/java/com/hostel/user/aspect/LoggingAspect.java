package com.hostel.user.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.hostel.user.service.*.*(..))")
    public void serviceLayerPointcut() {}

    @Pointcut("execution(* com.hostel.user.controller.*.*(..))")
    public void controllerLayerPointcut() {}

    @Before("serviceLayerPointcut()")
    public void logBeforeServiceMethod(JoinPoint joinPoint) {
        log.info("Entering method: {}.{} with arguments: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(pointcut = "serviceLayerPointcut()", returning = "result")
    public void logAfterServiceMethod(JoinPoint joinPoint, Object result) {
        log.info("Method {}.{} returned: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }

    @AfterThrowing(pointcut = "serviceLayerPointcut()", throwing = "exception")
    public void logServiceException(JoinPoint joinPoint, Exception exception) {
        log.error("Exception in {}.{}: {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                exception.getMessage(), exception);
    }

    @Around("controllerLayerPointcut()")
    public Object logControllerExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Controller {}.{} executed in {} ms",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                duration);
        return result;
    }
}
