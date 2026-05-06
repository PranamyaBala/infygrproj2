package com.hostel.user.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getDeclaringTypeName()).thenReturn("com.example.TestClass");
        lenient().when(signature.getName()).thenReturn("testMethod");
    }

    @Test
    void logBeforeServiceMethod_ShouldNotThrowException() {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});
        loggingAspect.logBeforeServiceMethod(joinPoint);
    }

    @Test
    void logAfterServiceMethod_ShouldNotThrowException() {
        loggingAspect.logAfterServiceMethod(joinPoint, "result");
    }

    @Test
    void logServiceException_ShouldNotThrowException() {
        loggingAspect.logServiceException(joinPoint, new RuntimeException("Test error"));
    }

    @Test
    void logControllerExecutionTime_ShouldReturnProceedResult() throws Throwable {
        when(proceedingJoinPoint.proceed()).thenReturn("proceedResult");
        Object result = loggingAspect.logControllerExecutionTime(proceedingJoinPoint);
        assertEquals("proceedResult", result);
        verify(proceedingJoinPoint, times(1)).proceed();
    }
}
