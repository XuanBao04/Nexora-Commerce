package com.nexoracommerce.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.nexoracommerce..service.impl.*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        log.info("==> Entering: {}.{} with arguments: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            log.info("<== Exiting: {}.{} | Execution Time: {} ms", className, methodName, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable throwable) {
            log.error("!!! Exception in {}.{} | After {} ms | Message: {}",
                    className, methodName, System.currentTimeMillis() - start, throwable.getMessage());
            throw throwable;
        }
    }
}
